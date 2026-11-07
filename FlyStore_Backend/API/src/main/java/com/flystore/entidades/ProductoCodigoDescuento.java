package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad intermedia ProductoCodigoDescuento - Relación Producto y CodigoDescuento
 */
@Entity
@Table(name = "producto_codigo_descuento")
@IdClass(ProductoCodigoDescuentoPK.class)
public class ProductoCodigoDescuento {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_descuento_id")
    private CodigoDescuento codigoDescuento;

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public CodigoDescuento getCodigoDescuento() {
        return codigoDescuento;
    }

    public void setCodigoDescuento(CodigoDescuento codigoDescuento) {
        this.codigoDescuento = codigoDescuento;
    }
    
    
}
