package com.flystore.modulos.catalogo.aplicacion.mappers;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.consultas.ConteoTalla;
import com.flystore.persistencia.consultas.OpcionesFiltroCatalogo;
import com.flystore.persistencia.consultas.OrdenCatalogo;
import com.flystore.persistencia.entidades.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Convierte productos y opciones de filtro del catálogo de la tienda (CU-01) a sus DTOs. */
@Component
public class CatalogoBOMapper {

    private static final List<String> TALLAS_LETRA = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL");
    private static final Pattern NUMERO = Pattern.compile("^(\\d+(?:[.,]\\d+)?)(?:\\s+(\\d+)/(\\d+))?$");
    private static final Pattern FRACCION = Pattern.compile("^(\\d+)/(\\d+)$");

    public LineaDTO aLineaDTO(Categoria categoria) {
        var nombre = categoria.getNombre() == null ? "" : categoria.getNombre().trim();
        return new LineaDTO(categoria.getId(), nombre, ruta(nombre), categoria.getLogoUrl());
    }

    /** Segmento de URL de la línea: «Gorras de prueba» → «gorras-de-prueba». */
    public static String ruta(String nombreLinea) {
        return nombreLinea == null ? "" : nombreLinea.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    public ProductoTarjetaDTO aProductoTarjetaDTO(Producto producto, PrecioProducto referencia) {
        return new ProductoTarjetaDTO(producto.getId(), producto.getClave(), producto.getNombre(),
                producto.getMarca() == null ? null : producto.getMarca().getNombre(),
                producto.getSubcategoria() == null ? null : producto.getSubcategoria().getNombre(),
                imagenPrincipal(producto), aPrecioReferenciaDTO(referencia));
    }

    public PrecioReferenciaDTO aPrecioReferenciaDTO(PrecioProducto precio) {
        if (precio == null) return null;
        return new PrecioReferenciaDTO(precio.getTipoPrecio().name(), precio.getPrecio(), precio.getMinPiezas());
    }

    public OpcionesFiltroRespuestaDTO aOpcionesFiltroRespuestaDTO(OpcionesFiltroCatalogo opciones) {
        var marcas = opciones.marcas().stream().map(o -> new OpcionFiltroDTO(o.id(), o.nombre(), o.cantidad())).toList();
        var subcategorias = opciones.subcategorias().stream().map(o -> new OpcionFiltroDTO(o.id(), o.nombre(), o.cantidad())).toList();
        var colores = opciones.colores().stream().map(c -> new ColorFiltroDTO(c.id(), c.nombre(), c.hex(), c.cantidad())).toList();
        var porTipo = new LinkedHashMap<Long, List<ConteoTalla>>();
        opciones.tallas().forEach(t -> porTipo.computeIfAbsent(t.idTipoTalla(), k -> new ArrayList<>()).add(t));
        var tiposTalla = porTipo.values().stream().map(tallas -> {
            var primera = tallas.get(0);
            var ordenadas = tallas.stream().sorted(Comparator.comparing(ConteoTalla::etiqueta, ORDEN_TALLAS))
                    .map(t -> new OpcionFiltroDTO(t.id(), t.etiqueta(), t.cantidad())).toList();
            return new TipoTallaFiltroDTO(primera.idTipoTalla(), primera.tipoTalla(), primera.unidadMedida(), ordenadas);
        }).toList();
        var unidades = opciones.tallas().stream().map(ConteoTalla::unidadMedida).filter(Objects::nonNull)
                .map(String::trim).filter(u -> !u.isEmpty()).distinct().toList();
        return new OpcionesFiltroRespuestaDTO(marcas, subcategorias, colores, tiposTalla, unidades,
                new RangoPrecioDTO(opciones.precioMinimo(), opciones.precioMaximo()), ordenes());
    }

    /** Opciones del selector «Ordenar por» del Figma. */
    public List<CatalogoItemDTO> ordenes() {
        return List.of(
                new CatalogoItemDTO(OrdenCatalogo.RECIENTES.name(), "Más recientes"),
                new CatalogoItemDTO(OrdenCatalogo.PRECIO_ASC.name(), "Precio: menor a mayor"),
                new CatalogoItemDTO(OrdenCatalogo.PRECIO_DESC.name(), "Precio: mayor a menor"),
                new CatalogoItemDTO(OrdenCatalogo.NOMBRE_ASC.name(), "Nombre A–Z"),
                new CatalogoItemDTO(OrdenCatalogo.MAS_VENDIDOS.name(), "Más vendidos"));
    }

    public CatalogoRespuestaDTO aCatalogoRespuestaDTO(Categoria categoria, List<ProductoTarjetaDTO> productos,
            OpcionesFiltroRespuestaDTO opciones, int pagina, int tamanoPagina, long total, TipoPrecio tipoPrecio,
            List<ProductoTarjetaDTO> sugerencias) {
        boolean hayMas = (long) (pagina + 1) * tamanoPagina < total;
        return new CatalogoRespuestaDTO(aLineaDTO(categoria), productos, opciones, pagina, tamanoPagina, total, hayMas,
                tipoPrecio.name(), sugerencias);
    }

    /**
     * @param niveles    esquemas válidos del producto ordenados por mínimo de piezas.
     * @param referencia esquema del tipo de cliente.
     * @param existencia piezas disponibles por variante (ya calculadas por el BO).
     */
    public ProductoDetalleRespuestaDTO aProductoDetalleRespuestaDTO(Producto producto, List<PrecioProducto> niveles,
            PrecioProducto referencia, java.util.function.ToIntFunction<VarianteProducto> existencia) {
        var categoria = producto.getSubcategoria().getCategoria();
        var imagenes = lista(producto.getImagenes()).stream()
                .sorted(Comparator.comparing(ImagenProducto::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ImagenProducto::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(i -> new ImagenProductoDTO(i.getUrlImagen(), i.getColor() == null ? null : i.getColor().getId(), i.getOrden()))
                .toList();
        var variantes = lista(producto.getVariantes()).stream()
                .sorted(Comparator.comparing((VarianteProducto v) -> v.getColor() == null ? "" : v.getColor().getNombreColor(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(v -> v.getTalla() == null ? "" : v.getTalla().getEtiqueta(), ORDEN_TALLAS))
                .map(v -> aVarianteVentaDTO(v, existencia.applyAsInt(v)))
                .toList();
        var precios = niveles.stream()
                .map(p -> new NivelPrecioCatalogoDTO(p.getTipoPrecio().name(), p.getMinPiezas(), p.getPrecio(), p == referencia))
                .toList();
        int existenciaTotal = variantes.stream().mapToInt(VarianteVentaDTO::existencia).sum();
        int cantidadInicial = Math.max(1, referencia.getMinPiezas() == null ? 1 : referencia.getMinPiezas());
        return new ProductoDetalleRespuestaDTO(producto.getId(), producto.getClave(), producto.getNombre(),
                producto.getDescripcion(), producto.getEstadoProducto() == null ? null : producto.getEstadoProducto().name(),
                aLineaDTO(categoria),
                new ElementoCatalogoDTO(producto.getMarca().getId(), producto.getMarca().getNombre()),
                new ElementoCatalogoDTO(producto.getSubcategoria().getId(), producto.getSubcategoria().getNombre()),
                imagenes, variantes, precios, aPrecioReferenciaDTO(referencia), cantidadInicial, existenciaTotal,
                existenciaTotal > 0);
    }

    public VarianteVentaDTO aVarianteVentaDTO(VarianteProducto v, int existencia) {
        var color = v.getColor();
        var talla = v.getTalla();
        var tipo = talla == null ? null : talla.getTipoTalla();
        return new VarianteVentaDTO(v.getId(), v.getSku(),
                color == null ? null : color.getId(), color == null ? null : color.getNombreColor(), color == null ? null : color.getHex(),
                talla == null ? null : talla.getId(), talla == null ? null : talla.getEtiqueta(),
                tipo == null ? null : tipo.getId(), tipo == null ? null : tipo.getNombre(), tipo == null ? null : tipo.getUnidadMedida(),
                existencia, existencia > 0);
    }

    /** Imagen con menor orden; null si el producto no tiene imágenes. */
    public String imagenPrincipal(Producto producto) {
        return lista(producto.getImagenes()).stream()
                .min(Comparator.comparing(ImagenProducto::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ImagenProducto::getUrlImagen).orElse(null);
    }

    /**
     * Orden natural de tallas: numéricas por valor («6 7/8» antes que «7», «22.5» antes que «23»),
     * luego de letra (XS, S, M, L, XL…; «S-M» por su primera talla) y al final el resto por nombre.
     */
    public static final Comparator<String> ORDEN_TALLAS = Comparator
            .comparingInt(CatalogoBOMapper::grupoTalla)
            .thenComparingDouble(CatalogoBOMapper::valorTalla)
            .thenComparing(t -> t == null ? "" : t.toLowerCase(Locale.ROOT));

    private static int grupoTalla(String talla) {
        if (numero(talla) != null) return 0;
        if (indiceLetra(talla) >= 0) return 1;
        return 2;
    }

    private static double valorTalla(String talla) {
        var numero = numero(talla);
        if (numero != null) return numero;
        int indice = indiceLetra(talla);
        return indice >= 0 ? indice : 0;
    }

    private static Double numero(String talla) {
        if (talla == null) return null;
        var texto = talla.trim();
        var fraccion = FRACCION.matcher(texto);
        if (fraccion.matches()) return Double.parseDouble(fraccion.group(1)) / Double.parseDouble(fraccion.group(2));
        var m = NUMERO.matcher(texto);
        if (!m.matches()) return null;
        double valor = Double.parseDouble(m.group(1).replace(',', '.'));
        if (m.group(2) != null && !"0".equals(m.group(3))) valor += Double.parseDouble(m.group(2)) / Double.parseDouble(m.group(3));
        return valor;
    }

    private static int indiceLetra(String talla) {
        if (talla == null) return -1;
        var primera = talla.trim().toUpperCase(Locale.ROOT).split("[-/\\s]")[0];
        return TALLAS_LETRA.indexOf(primera);
    }

    private static <T> List<T> lista(List<T> valores) {
        return valores == null ? List.of() : valores;
    }
}
