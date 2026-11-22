package com.flystore.modulos.gestionarventa;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.modulos.gestionarventa.aplicacion.DetallePedidoBO;
import com.flystore.modulos.gestionarventa.aplicacion.ProductoVentaBO;
import com.flystore.persistencia.entidades.PedidoVenta;
import com.flystore.persistencia.entidades.Producto;
import com.flystore.persistencia.entidades.TipoPrecio;
import com.flystore.persistencia.entidades.VarianteProducto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DetallePedidoBOTest {
    private final DetallePedidoBO bo = new DetallePedidoBO(new ProductoVentaBO());
    private final Datos datos = new Datos();

    /** Datos compartidos: un carrito y una gorra con dos colores. */
    private static final class Datos {
        final PedidoVenta pedido = DatosVenta.carrito(20, DatosVenta.cliente(7));
        final Producto gorra = DatosVenta.gorra(1);
        final VarianteProducto roja = DatosVenta.variante(10, gorra, "Rojo", 50);
        final VarianteProducto negra = DatosVenta.variante(11, gorra, "Negro", 50);
    }

    @Test void lineaNuevaQuedaLigadaAlPedidoYALaVariante() {
        bo.agregarVariante(datos.pedido, datos.roja, 2);
        var linea = datos.pedido.getDetalles().get(0);
        assertSame(datos.pedido, linea.getPedidoVenta());
        assertEquals(1, linea.getVariantes().size());
        assertSame(linea, linea.getVariantes().get(0).getDetallePedido());
        assertSame(datos.roja, linea.getVariantes().get(0).getVarianteProducto());
        assertEquals(1, linea.getVariantes().get(0).getCantidadEnCombo(), "Una pieza de la variante por unidad de la línea");
    }

    @Test void agregarLaMismaVarianteSumaPiezas() {
        bo.agregarVariante(datos.pedido, datos.roja, 2);
        assertEquals(5, bo.agregarVariante(datos.pedido, datos.roja, 3));
        assertEquals(1, datos.pedido.getDetalles().size());
    }

    @Test void elPrecioSeCalculaSumandoLasVariantesDelProducto() {
        bo.agregarVariante(datos.pedido, datos.roja, 3);
        bo.agregarVariante(datos.pedido, datos.negra, 3);
        bo.recalcularPrecios(datos.pedido);
        for (var linea : datos.pedido.getDetalles()) {
            assertEquals(TipoPrecio.MAYOREO, linea.getTipoPrecio());
            assertEquals(0, new BigDecimal("130.00").compareTo(linea.getPrecioUnitario()));
        }
        bo.actualizarCantidad(datos.pedido, datos.negra.getId(), 2);
        bo.recalcularPrecios(datos.pedido);
        assertEquals(TipoPrecio.BASE, datos.pedido.getDetalles().get(0).getTipoPrecio());
    }

    @Test void cantidadCeroQuitaLaLinea() {
        bo.agregarVariante(datos.pedido, datos.roja, 2);
        bo.actualizarCantidad(datos.pedido, datos.roja.getId(), 0);
        assertTrue(datos.pedido.getDetalles().isEmpty());
        assertThrows(NegocioException.class, () -> bo.actualizarCantidad(datos.pedido, datos.roja.getId(), 1));
    }

    @Test void elCodigoRestaPorPiezaSinSuperarElPrecio() {
        bo.agregarVariante(datos.pedido, datos.roja, 2);
        var codigo = DatosVenta.codigo(3, "FLY10", "200.00");
        assertEquals(1, bo.aplicarCodigoDescuento(datos.pedido, codigo, Set.of(datos.gorra.getId())));
        bo.recalcularPrecios(datos.pedido);
        var linea = datos.pedido.getDetalles().get(0);
        assertEquals(0, new BigDecimal("150.00").compareTo(linea.getDescuentoUnitario()), "El descuento no deja precios negativos");
        assertEquals(0, BigDecimal.ZERO.compareTo(bo.calcularSubtotal(linea)));
        bo.quitarCodigoDescuento(datos.pedido, codigo);
        bo.recalcularPrecios(datos.pedido);
        assertEquals(0, BigDecimal.ZERO.compareTo(linea.getDescuentoUnitario()));
    }

    @Test void codigoDeOtroProductoNoSeAplica() {
        bo.agregarVariante(datos.pedido, datos.roja, 2);
        assertEquals(0, bo.aplicarCodigoDescuento(datos.pedido, DatosVenta.codigo(3, "OTRO", "10"), Set.of(99L)));
    }
}
