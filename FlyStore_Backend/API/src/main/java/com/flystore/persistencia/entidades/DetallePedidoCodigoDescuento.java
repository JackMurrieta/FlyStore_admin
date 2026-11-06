package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad intermedia DetallePedidoCodigoDescuento - Relación DetallePedido y CodigoDescuento
 */
@Entity
@Table(name = "detalle_pedido_codigo_descuento")
@IdClass(DetallePedidoCodigoDescuentoPK.class)
public class DetallePedidoCodigoDescuento {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_pedido_id")
    private DetallePedido detallePedido;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "codigo_descuento_id")
    private CodigoDescuento codigoDescuento;

    public DetallePedido getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(DetallePedido detallePedido) {
        this.detallePedido = detallePedido;
    }

    public CodigoDescuento getCodigoDescuento() {
        return codigoDescuento;
    }

    public void setCodigoDescuento(CodigoDescuento codigoDescuento) {
        this.codigoDescuento = codigoDescuento;
    }
    
    
}
