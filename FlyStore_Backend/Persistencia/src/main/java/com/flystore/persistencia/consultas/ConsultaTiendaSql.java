package com.flystore.persistencia.consultas;

import jakarta.persistence.Query;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Arma el SQL de las consultas del catálogo de la tienda a partir de un {@link FiltroCatalogo}.
 * Todas las consultas (página de productos, total, opciones de los filtros y sugerencias) parten
 * del mismo alcance: productos visibles de la línea que cumplen el texto y los filtros.
 *
 * <p>Es SQL nativo de PostgreSQL porque usa {@code translate} para ignorar acentos y subconsultas
 * correlacionadas para el precio de referencia y las piezas vendidas. Los valores del usuario
 * siempre van como parámetros; solo las constantes de estados se escriben en el texto.
 *
 * <p>Vive fuera del paquete dao a propósito: MapeoJpaTest valida como HQL los textos que empiezan
 * con «select» en los DAO, y estas consultas son SQL nativo.
 */
public final class ConsultaTiendaSql {

    /** Estados de producto que se muestran en la tienda. */
    public static final String ESTADOS_VISIBLES = "'PUBLICADO', 'STOCK_BAJO'";
    /** Estados de pedido que cuentan como venta para «Más vendidos». */
    public static final String ESTADOS_VENDIDOS = "'PAGADO', 'PREPARACION', 'ENVIADO', 'ENTREGADO'";

    private static final String CON_ACENTO = "áàäâãéèëêíìïîóòöôõúùüûñç";
    private static final String SIN_ACENTO = "aaaaaeeeeiiiiooooouuuunc";

    /** Producto visible: estado público, alguna variante con existencia y al menos un precio. */
    public static final String VISIBLE = "cast(p.estado_producto as text) in (" + ESTADOS_VISIBLES + ")"
            + " and exists (select 1 from variante_producto v0 where v0.producto_id = p.id and v0.stock > 0)"
            + " and exists (select 1 from precio_producto pp0 where pp0.producto_id = p.id"
            + " and pp0.precio is not null and pp0.min_piezas is not null and pp0.tipo_precio is not null)";

    /** Esquema de precio completo (los demás no se pueden cobrar). */
    private static final String PRECIO_VALIDO = "pp.precio is not null and pp.min_piezas is not null and pp.tipo_precio is not null";

    /** Texto donde se busca: nombre, clave (modelo), marca, subcategoría y línea, en minúsculas y sin acentos. */
    private static final String TEXTO = "translate(lower(coalesce(p.nombre, '') || ' ' || coalesce(p.clave, '') || ' '"
            + " || coalesce(m.nombre, '') || ' ' || coalesce(s.nombre, '') || ' ' || coalesce(c.nombre, '')), '"
            + CON_ACENTO + "', '" + SIN_ACENTO + "')";

    /**
     * Precio del esquema de referencia (el del tipo de cliente). Si el producto no lo tiene, el
     * precio BASE; si tampoco, el del esquema con menos piezas. Es la misma regla que aplica Negocio.
     */
    private static final String PRECIO_REFERENCIA = "coalesce("
            + "(select pp.precio from precio_producto pp where pp.producto_id = p.id and " + PRECIO_VALIDO
            + " and cast(pp.tipo_precio as text) = :tipoPrecio order by pp.min_piezas, pp.precio limit 1), "
            + "(select pp.precio from precio_producto pp where pp.producto_id = p.id and " + PRECIO_VALIDO
            + " and cast(pp.tipo_precio as text) = 'BASE' order by pp.min_piezas, pp.precio limit 1), "
            + "(select pp.precio from precio_producto pp where pp.producto_id = p.id and " + PRECIO_VALIDO
            + " order by pp.min_piezas, pp.precio limit 1))";

    /** Piezas vendidas del producto en pedidos con pago confirmado. */
    private static final String VENDIDOS = "(select coalesce(sum(dp.cantidad), 0) from detalle_pedido dp"
            + " join detalle_pedido_variante_producto dpv on dpv.detalle_pedido_id = dp.id"
            + " join variante_producto vv on vv.id = dpv.variante_producto_id"
            + " join pedido_venta pv on pv.id = dp.pedido_venta_id"
            + " where vv.producto_id = p.id and cast(pv.estado_venta as text) in (" + ESTADOS_VENDIDOS + "))";

    private final FiltroCatalogo filtro;
    private final StringBuilder condiciones = new StringBuilder(VISIBLE);
    private final Map<String, Object> parametros = new LinkedHashMap<>();

    public ConsultaTiendaSql(FiltroCatalogo filtro) {
        this.filtro = filtro;
        parametros.put("tipoPrecio", filtro.tipoPrecio().name());
        if (filtro.idCategoria() != null) {
            condiciones.append(" and c.id = :categoria");
            parametros.put("categoria", filtro.idCategoria());
        }
        agregarTexto();
        if (!filtro.marcas().isEmpty()) {
            condiciones.append(" and p.marca_id in (:marcas)");
            parametros.put("marcas", filtro.marcas());
        }
        if (!filtro.subcategorias().isEmpty()) {
            condiciones.append(" and p.subcategoria_id in (:subcategorias)");
            parametros.put("subcategorias", filtro.subcategorias());
        }
        agregarFiltroVariantes();
    }

    /** Ids de los productos en el orden pedido; la página se aplica con setFirstResult/setMaxResults. */
    public String idsOrdenados() {
        return "select x.id from (" + alcance(filtro.orden() == OrdenCatalogo.MAS_VENDIDOS) + ") x"
                + filtroPrecio() + " order by " + orden();
    }

    /** Total de productos que cumplen el filtro, sin paginar. */
    public String contar() {
        return "select count(*) from (" + alcance(false) + ") x" + filtroPrecio();
    }

    public String conteoMarcas() {
        return "select m.id, m.nombre, m.logo_url, count(*) from (" + alcance(false) + ") x"
                + " join producto p on p.id = x.id join marca m on m.id = p.marca_id"
                + " group by m.id, m.nombre, m.logo_url order by lower(m.nombre), m.id";
    }

    /** Productos del alcance por línea (búsqueda en toda la tienda: «12 coincidencias en Gorras»). */
    public String conteoLineas() {
        return "select c.id, c.nombre, count(*) from (" + alcance(false) + ") x"
                + " join producto p on p.id = x.id join subcategoria s on s.id = p.subcategoria_id"
                + " join categoria c on c.id = s.categoria_id"
                + " group by c.id, c.nombre order by count(*) desc, lower(c.nombre), c.id";
    }

    public String conteoSubcategorias() {
        return "select s.id, s.nombre, count(*) from (" + alcance(false) + ") x"
                + " join producto p on p.id = x.id join subcategoria s on s.id = p.subcategoria_id"
                + " group by s.id, s.nombre order by lower(s.nombre), s.id";
    }

    /** Colores con existencia entre los productos del alcance. */
    public String conteoColores() {
        return "select co.id, co.nombre_color, co.hex, count(distinct x.id) from (" + alcance(false) + ") x"
                + " join variante_producto v on v.producto_id = x.id and v.stock > 0"
                + " join color co on co.id = v.color_id"
                + " group by co.id, co.nombre_color, co.hex order by lower(co.nombre_color), co.id";
    }

    /** Tallas con existencia entre los productos del alcance, con su tipo de talla y unidad de medida. */
    public String conteoTallas() {
        return "select t.id, t.etiqueta, tt.id, tt.nombre, tt.unidad_medida, count(distinct x.id) from (" + alcance(false) + ") x"
                + " join variante_producto v on v.producto_id = x.id and v.stock > 0"
                + " join talla t on t.id = v.talla_id join tipo_talla tt on tt.id = t.tipo_talla_id"
                + " group by t.id, t.etiqueta, tt.id, tt.nombre, tt.unidad_medida order by tt.id, t.id";
    }

    /** Precio de referencia mínimo y máximo del alcance. */
    public String rangoPrecio() {
        return "select min(x.precio_ref), max(x.precio_ref) from (" + alcance(false) + ") x";
    }

    /** ¿El producto se muestra en la tienda? */
    public static String esVisible() {
        return "select count(*) from producto p where p.id = :idProducto and " + VISIBLE;
    }

    public <Q extends Query> Q aplicar(Q query) {
        parametros.forEach(query::setParameter);
        if (filtro.precioMinimo() != null) query.setParameter("precioMinimo", filtro.precioMinimo());
        if (filtro.precioMaximo() != null) query.setParameter("precioMaximo", filtro.precioMaximo());
        return query;
    }

    /** Aplica solo los parámetros del alcance (las opciones de filtros no usan el rango de precio). */
    public <Q extends Query> Q aplicarAlcance(Q query) {
        parametros.forEach(query::setParameter);
        return query;
    }

    public Map<String, Object> parametros() {
        return java.util.Collections.unmodifiableMap(parametros);
    }

    private String alcance(boolean conVendidos) {
        return "select p.id as id, p.nombre as nombre, " + PRECIO_REFERENCIA + " as precio_ref"
                + (conVendidos ? ", " + VENDIDOS + " as vendidos" : "")
                + " from producto p"
                + " join subcategoria s on s.id = p.subcategoria_id"
                + " join categoria c on c.id = s.categoria_id"
                + " join marca m on m.id = p.marca_id"
                + " where " + condiciones;
    }

    private String filtroPrecio() {
        var sql = new StringBuilder();
        if (filtro.precioMinimo() != null) sql.append(" where x.precio_ref >= :precioMinimo");
        if (filtro.precioMaximo() != null) sql.append(sql.isEmpty() ? " where" : " and").append(" x.precio_ref <= :precioMaximo");
        return sql.toString();
    }

    private String orden() {
        return switch (filtro.orden()) {
            case PRECIO_ASC -> "x.precio_ref asc, x.id desc";
            case PRECIO_DESC -> "x.precio_ref desc, x.id desc";
            case NOMBRE_ASC -> "lower(x.nombre) asc nulls last, x.id asc";
            case MAS_VENDIDOS -> "x.vendidos desc, x.id desc";
            case RECIENTES -> "x.id desc";
        };
    }

    private void agregarTexto() {
        if (!filtro.tieneTexto()) return;
        var union = filtro.cualquierTermino() ? " or " : " and ";
        condiciones.append(" and (");
        for (int i = 0; i < filtro.terminos().size(); i++) {
            if (i > 0) condiciones.append(union);
            condiciones.append("strpos(").append(TEXTO).append(", :t").append(i).append(") > 0");
            parametros.put("t" + i, filtro.terminos().get(i));
        }
        condiciones.append(")");
    }

    /** Talla, color y unidad de medida se cumplen en la misma variante y con existencia. */
    private void agregarFiltroVariantes() {
        if (!filtro.filtraVariantes()) return;
        condiciones.append(" and exists (select 1 from variante_producto vf")
                .append(" left join talla tf on tf.id = vf.talla_id")
                .append(" left join tipo_talla ttf on ttf.id = tf.tipo_talla_id")
                .append(" where vf.producto_id = p.id and vf.stock > 0");
        if (!filtro.colores().isEmpty()) {
            condiciones.append(" and vf.color_id in (:colores)");
            parametros.put("colores", filtro.colores());
        }
        if (!filtro.tallas().isEmpty()) {
            condiciones.append(" and vf.talla_id in (:tallas)");
            parametros.put("tallas", filtro.tallas());
        }
        if (!filtro.unidadesMedida().isEmpty()) {
            condiciones.append(" and lower(ttf.unidad_medida) in (:unidades)");
            parametros.put("unidades", filtro.unidadesMedida());
        }
        condiciones.append(")");
    }
}
