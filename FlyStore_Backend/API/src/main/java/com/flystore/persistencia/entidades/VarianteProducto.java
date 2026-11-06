package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad VarianteProducto - Un producto tiene muchas variantes (combinaciones de color/talla)
 */
@Entity
@Table(name = "variante_producto")
public class VarianteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "talla_id")
    private Talla talla;

    @Column(unique = true)
    private String sku;

    @Column
    private Integer stock;
}
