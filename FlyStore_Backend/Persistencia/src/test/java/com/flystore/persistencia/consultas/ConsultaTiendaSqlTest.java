package com.flystore.persistencia.consultas;

import com.flystore.persistencia.entidades.TipoPrecio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL del catálogo de la tienda (CU-01). Las consultas se probaron contra el esquema real de Supabase;
 * aquí se cuida que los valores del usuario vayan como parámetros y que cada filtro arme su condición.
 */
class ConsultaTiendaSqlTest {

    private static FiltroCatalogo filtro(List<String> terminos, Set<Long> marcas, Set<Long> tallas, Set<Long> colores,
            Set<String> unidades, BigDecimal minimo, BigDecimal maximo, OrdenCatalogo orden) {
        return new FiltroCatalogo(2L, terminos, false, marcas, Set.of(), tallas, colores, unidades, minimo, maximo,
                TipoPrecio.MAYOREO, orden);
    }

    @Test void soloProductosVisiblesDeLaLineaSinFiltrosExtra() {
        var sql = new ConsultaTiendaSql(FiltroCatalogo.deLinea(2L, TipoPrecio.BASE));
        var ids = sql.idsOrdenados();
        assertTrue(ids.contains("cast(p.estado_producto as text) in ('PUBLICADO', 'STOCK_BAJO')"));
        assertTrue(ids.contains("v0.stock > 0"), "solo con existencia");
        assertTrue(ids.contains("pp0.precio is not null"), "solo con precio");
        assertTrue(ids.contains("c.id = :categoria"));
        assertFalse(ids.contains("vf."), "sin filtro de variantes no hay subconsulta de variantes");
        assertFalse(ids.contains(":precioMinimo"));
        assertTrue(ids.endsWith("order by x.id desc"), "por defecto, más recientes");
        assertEquals(2L, sql.parametros().get("categoria"));
        assertEquals("BASE", sql.parametros().get("tipoPrecio"));
    }

    @Test void textoVaComoParametroYTodasLasPalabrasDebenEstar() {
        var sql = new ConsultaTiendaSql(filtro(List.of("9forty", "negro'; drop table producto; --"), Set.of(), Set.of(), Set.of(),
                Set.of(), null, null, OrdenCatalogo.RECIENTES));
        var texto = sql.idsOrdenados();
        assertFalse(texto.contains("drop table"), "el texto del cliente nunca se concatena al SQL");
        assertTrue(texto.contains("strpos(") && texto.contains(":t0") && texto.contains(":t1"));
        assertTrue(texto.contains(") > 0 and strpos("), "todas las palabras");
        assertTrue(texto.contains("translate(lower("), "sin importar mayúsculas ni acentos");
        assertEquals("9forty", sql.parametros().get("t0"));
    }

    @Test void coincidenciaParcialBastaConUnaPalabraYOrdenaPorMasVendidos() {
        var parcial = filtro(List.of("gorra", "rosa"), Set.of(1L), Set.of(), Set.of(), Set.of(), null, null, OrdenCatalogo.PRECIO_ASC)
                .coincidenciaParcial();
        var sql = new ConsultaTiendaSql(parcial).idsOrdenados();
        assertTrue(sql.contains(") > 0 or strpos("));
        assertFalse(sql.contains("p.marca_id in"), "las sugerencias no conservan los demás filtros");
        assertTrue(sql.contains("as vendidos") && sql.endsWith("order by x.vendidos desc, x.id desc"));
        assertTrue(sql.contains("'PAGADO', 'PREPARACION', 'ENVIADO', 'ENTREGADO'"), "vendidos = pedidos con pago confirmado");
    }

    @Test void tallaColorYUnidadSeCumplenEnLaMismaVarianteConExistencia() {
        var sql = new ConsultaTiendaSql(filtro(List.of(), Set.of(), Set.of(7L), Set.of(9L), Set.of("in"), null, null, OrdenCatalogo.RECIENTES));
        var texto = sql.contar();
        assertEquals(1, texto.split("from variante_producto vf", -1).length - 1, "una sola subconsulta para los tres filtros");
        assertTrue(texto.contains("vf.stock > 0 and vf.color_id in (:colores) and vf.talla_id in (:tallas) and lower(ttf.unidad_medida) in (:unidades)"));
        assertEquals(Set.of(9L), sql.parametros().get("colores"));
    }

    @Test void rangoDePrecioUsaElPrecioDeReferenciaEnPaginaYTotal() {
        var sql = new ConsultaTiendaSql(filtro(List.of(), Set.of(), Set.of(), Set.of(), Set.of(), new BigDecimal("150"),
                new BigDecimal("300"), OrdenCatalogo.PRECIO_DESC));
        assertTrue(sql.idsOrdenados().contains(" where x.precio_ref >= :precioMinimo and x.precio_ref <= :precioMaximo order by x.precio_ref desc"));
        assertTrue(sql.contar().endsWith(" where x.precio_ref >= :precioMinimo and x.precio_ref <= :precioMaximo"));
        assertTrue(sql.idsOrdenados().contains("cast(pp.tipo_precio as text) = :tipoPrecio"), "precio del tipo de cliente");
        assertTrue(sql.idsOrdenados().contains("cast(pp.tipo_precio as text) = 'BASE'"), "si no lo tiene, Base");
    }

    @Test void opcionesDeFiltroNoUsanElRangoDePrecio() {
        var alcance = filtro(List.of("gorra"), Set.of(1L), Set.of(), Set.of(), Set.of(), new BigDecimal("1"), null, OrdenCatalogo.RECIENTES).soloLinea();
        var sql = new ConsultaTiendaSql(alcance);
        for (var consulta : List.of(sql.conteoMarcas(), sql.conteoSubcategorias(), sql.conteoColores(), sql.conteoTallas(), sql.rangoPrecio())) {
            assertFalse(consulta.contains(":precioMinimo"), consulta);
            assertFalse(consulta.contains("strpos("), "las opciones son de la línea completa");
            assertFalse(consulta.contains("p.marca_id in"));
        }
        assertTrue(sql.conteoColores().contains("v.stock > 0"), "solo colores con existencia");
        assertTrue(sql.conteoTallas().contains("tt.unidad_medida"));
    }

    @Test void marcasTraenSuLogoYLaBusquedaCuentaPorLinea() {
        var sql = new ConsultaTiendaSql(filtro(List.of("9forty"), Set.of(1L), Set.of(), Set.of(), Set.of(), null, null,
                OrdenCatalogo.RECIENTES).soloTexto());
        assertTrue(sql.conteoMarcas().startsWith("select m.id, m.nombre, m.logo_url, count(*)"));
        var lineas = sql.conteoLineas();
        assertTrue(lineas.startsWith("select c.id, c.nombre, count(*)"));
        assertTrue(lineas.contains("strpos("), "cuenta los resultados del texto buscado");
        assertFalse(lineas.contains("c.id = :categoria"), "en toda la tienda");
        assertFalse(lineas.contains("p.marca_id in"), "sin los demás filtros");
    }

    @Test void cadaOrdenTieneDesempateEstable() {
        for (var orden : OrdenCatalogo.values()) {
            var sql = new ConsultaTiendaSql(filtro(List.of(), Set.of(), Set.of(), Set.of(), Set.of(), null, null, orden)).idsOrdenados();
            assertTrue(sql.matches("(?s).*order by .*x\\.id( desc| asc)?$"), orden + ": " + sql.substring(sql.lastIndexOf("order by")));
        }
    }
}
