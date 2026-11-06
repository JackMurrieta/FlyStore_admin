package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad ImagenProducto - Muchas imagenes pertenecen a un producto y color
 */
@Entity
@Table(name = "imagen_producto")
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    @Column(name = "url_imagen")
    private String urlImagen;

    @Column
    private Integer orden;
}
