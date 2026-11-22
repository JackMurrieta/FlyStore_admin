package com.flystore.modulos.gestionarventa;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.gestionarventa.aplicacion.ProductoVentaBO;
import com.flystore.persistencia.entidades.Producto;
import com.flystore.persistencia.entidades.TipoPrecio;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductoVentaBOTest {
    private final ProductoVentaBO bo = new ProductoVentaBO();
    private final Producto gorra = DatosVenta.gorra(1);

    @Test void eligeElEsquemaConElMayorMinimoAlcanzado() {
        assertEquals(TipoPrecio.BASE, bo.determinarTipoPrecio(gorra, 5).getTipoPrecio());
        assertEquals(TipoPrecio.MAYOREO, bo.determinarTipoPrecio(gorra, 6).getTipoPrecio());
        assertEquals(TipoPrecio.MAYOREO, bo.determinarTipoPrecio(gorra, 11).getTipoPrecio());
        assertEquals(TipoPrecio.DISTRIBUIDOR, bo.determinarTipoPrecio(gorra, 12).getTipoPrecio());
    }

    @Test void informaPrecioBaseYSiguienteEsquema() {
        assertEquals(0, new BigDecimal("150.00").compareTo(bo.obtenerPrecioBase(gorra)));
        var siguiente = bo.obtenerSiguienteTipoPrecio(gorra, 7).orElseThrow();
        assertEquals(TipoPrecio.DISTRIBUIDOR, siguiente.getTipoPrecio());
        assertTrue(bo.obtenerSiguienteTipoPrecio(gorra, 12).isEmpty());
        assertEquals(List.of(1, 6, 12), bo.obtenerNivelesPrecio(gorra).stream().map(p -> p.getMinPiezas()).toList());
    }

    @Test void productoSinPrecioNoSePuedeVender() {
        gorra.setPrecios(List.of());
        var error = assertThrows(NegocioException.class, () -> bo.determinarTipoPrecio(gorra, 1));
        assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
    }
}
