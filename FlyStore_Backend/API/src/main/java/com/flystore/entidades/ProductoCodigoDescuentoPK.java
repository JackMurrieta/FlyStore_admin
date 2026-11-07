package com.flystore.entidades;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase PK compuesta para ProductoCodigoDescuento
 */
public class ProductoCodigoDescuentoPK implements Serializable {

    private Long producto;
    private Long codigoDescuento;

    public ProductoCodigoDescuentoPK() {
    }

    public ProductoCodigoDescuentoPK(Long producto, Long codigoDescuento) {
        this.producto = producto;
        this.codigoDescuento = codigoDescuento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductoCodigoDescuentoPK that = (ProductoCodigoDescuentoPK) o;
        return Objects.equals(producto, that.producto) &&
               Objects.equals(codigoDescuento, that.codigoDescuento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(producto, codigoDescuento);
    }
}
