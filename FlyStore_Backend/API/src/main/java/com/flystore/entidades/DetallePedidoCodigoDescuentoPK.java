package com.flystore.entidades;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase PK compuesta para DetallePedidoCodigoDescuento
 */
public class DetallePedidoCodigoDescuentoPK implements Serializable {

    private Long detallePedido;
    private Long codigoDescuento;

    public DetallePedidoCodigoDescuentoPK() {
    }

    public DetallePedidoCodigoDescuentoPK(Long detallePedido, Long codigoDescuento) {
        this.detallePedido = detallePedido;
        this.codigoDescuento = codigoDescuento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetallePedidoCodigoDescuentoPK that = (DetallePedidoCodigoDescuentoPK) o;
        return Objects.equals(detallePedido, that.detallePedido) &&
               Objects.equals(codigoDescuento, that.codigoDescuento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detallePedido, codigoDescuento);
    }
}
