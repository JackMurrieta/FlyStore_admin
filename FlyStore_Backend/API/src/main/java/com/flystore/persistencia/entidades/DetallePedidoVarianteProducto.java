package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad intermedia DetallePedidoVarianteProducto - Relación DetallePedido y VarianteProducto para combos/paquetes
 */
@Entity
@Table(name = "detalle_pedido_variante_producto")
public class DetallePedidoVarianteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_pedido_id", nullable = false)
    private DetallePedido detallePedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_producto_id", nullable = false)
    private VarianteProducto varianteProducto;

    @Column(name = "cantidad_en_combo", nullable = false)
    private Integer cantidadEnCombo;
}
