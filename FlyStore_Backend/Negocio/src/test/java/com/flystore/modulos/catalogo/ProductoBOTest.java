package com.flystore.modulos.catalogo;

import com.flystore.contratos.catalogo.*;
import com.flystore.dominio.excepcion.*;
import com.flystore.modulos.catalogo.aplicacion.ProductoBO;
import com.flystore.modulos.catalogo.aplicacion.mappers.ProductoMapper;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

class ProductoBOTest {
    @Test void buscarSinFiltrosSeRechazaAntesDeConsultarPersistencia() {
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> { throw new AssertionError("No debe consultar el DAO"); }), false);
        var error = assertThrows(NegocioException.class, () -> bo.buscarProductos("   ", null, "", " ", null));
        assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
        assertTrue(error.getMessage().contains("al menos un filtro"));
    }
    private CrearProductoDTO datos(List<VarianteDTO> variantes, List<PrecioProductoDTO> precios) {
        return new CrearProductoDTO("GORRA-01", "Gorra", "Gorra bordada", "1", "2", "3",
                List.of("https://example.com/gorra.jpg"), "PUBLICADO", variantes, precios);
    }
    private CrearProductoDTO datos() {
        return datos(List.of(new VarianteDTO("M", "Rojo", 5)),
                List.of(new PrecioProductoDTO("BASE", new BigDecimal("100.00"), 1)));
    }
    private ProductoBO bo(IProductoDAO dao, boolean ventas) { return new ProductoBO(dao, id -> ventas, new ProductoMapper()); }

    @Test void altaConservaIdsLongYMapeaTodoElAgregado() {
        var guardado = new AtomicReference<Producto>();
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            if (m.equals("guardar")) { guardado.set((Producto) a[0]); return null; }
            throw new AssertionError(m);
        }), false);
        bo.crearProducto(datos());
        var p = guardado.get();
        assertEquals(2L, p.getSubcategoria().getId());
        assertEquals(1L, p.getSubcategoria().getCategoria().getId());
        assertEquals(3L, p.getMarca().getId());
        assertEquals("GORRA-01", p.getClave());
        assertEquals("Rojo", p.getVariantes().getFirst().getColor().getNombreColor());
        assertEquals("M", p.getVariantes().getFirst().getTalla().getEtiqueta());
        assertNotNull(p.getVariantes().getFirst().getSku());
        assertEquals(new BigDecimal("100.00"), p.getPrecios().getFirst().getPrecio());
        assertEquals(0, p.getImagenes().getFirst().getOrden());
        assertNull(p.getImagenes().getFirst().getColor());
    }

    @Test void claveDuplicadaNoSeGuarda() {
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return true;
            throw new AssertionError("No debe guardar: " + m);
        }), false);
        assertEquals(TipoError.RECURSO_YA_EXISTE, assertThrows(NegocioException.class, () -> bo.crearProducto(datos())).getTipoError());
    }

    @Test void rechazaStockNegativoYVariantesRepetidasAntesDePersistir() {
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            throw new AssertionError("No debe guardar: " + m);
        }), false);
        assertThrows(NegocioException.class, () -> bo.crearProducto(datos(
                List.of(new VarianteDTO("M", "Rojo", -1)), datos().preciosProducto())));
        assertThrows(NegocioException.class, () -> bo.crearProducto(datos(
                List.of(new VarianteDTO("M", "Rojo", 1), new VarianteDTO(" m ", "ROJO", 3)), datos().preciosProducto())));
    }

    @Test void rechazaPreciosIncoherentesSinGuardar() {
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            throw new AssertionError("No debe guardar: " + m);
        }), false);
        for (var precios : List.of(
                List.of(new PrecioProductoDTO("BASE", BigDecimal.ZERO, 1)),
                List.of(new PrecioProductoDTO("MAYOREO", BigDecimal.TEN, 10)),
                List.of(new PrecioProductoDTO("BASE", BigDecimal.TEN, 1), new PrecioProductoDTO("MAYOREO", new BigDecimal("20"), 5)),
                List.of(new PrecioProductoDTO("BASE", BigDecimal.TEN, 1), new PrecioProductoDTO("BASE", BigDecimal.ONE, 5)))) {
            assertThrows(NegocioException.class, () -> bo.crearProducto(datos(datos().variantes(), precios)));
        }
    }

    @Test void productoConVentasSeInactivaSinEliminarse() {
        var inactivo = new AtomicBoolean();
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("obtenerPorId")) return Optional.of(new Producto());
            if (m.equals("cambiarEstado")) { assertEquals(7L, a[0]); assertEquals(EstadoProducto.INACTIVO, a[1]); inactivo.set(true); return null; }
            throw new AssertionError(m);
        }), true);
        assertEquals(new ResultadoBajaDTO("7", "LOGICA"), bo.darDeBajaProducto("7"));
        assertTrue(inactivo.get());
    }

    @Test void productoSinVentasSeElimina() {
        var eliminado = new AtomicBoolean();
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("obtenerPorId")) return Optional.of(new Producto());
            if (m.equals("eliminar")) { eliminado.set(true); return null; }
            throw new AssertionError(m);
        }), false);
        assertEquals(new ResultadoBajaDTO("7", "FISICA"), bo.darDeBajaProducto("7"));
        assertTrue(eliminado.get());
    }

    @Test void bajaInexistenteDevuelveNoEncontrado() {
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> Optional.empty()), false);
        assertEquals(TipoError.RECURSO_NO_ENCONTRADO, assertThrows(NegocioException.class, () -> bo.darDeBajaProducto("7")).getTipoError());
    }

    @Test void filtrosConviertenIdsSinTruncarValoresLong() {
        var bo = bo(puerto(IProductoDAO.class, (m,a) -> {
            assertEquals("buscar", m);
            assertEquals(9223372036854775807L, a[1]);
            assertNull(a[2]); assertEquals(3L, a[3]); assertEquals(EstadoProducto.BORRADOR, a[4]);
            return List.of();
        }), false);
        assertEquals(List.of(), bo.buscarProductos("gorra", "9223372036854775807", null, "3", "BORRADOR"));
        assertThrows(NegocioException.class, () -> bo.buscarProductos(null, "9223372036854775808", null, null, null));
        assertThrows(NegocioException.class, () -> bo.darDeBajaProducto("-1"));
    }
}
