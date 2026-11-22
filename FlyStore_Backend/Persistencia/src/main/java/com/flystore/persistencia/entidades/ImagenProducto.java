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
    @JoinColumn(name = "color_id")
    private Color color;

    @Column(name = "url_imagen")
    private String urlImagen;

    @Column
    private Integer orden;

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
    
    
    public void setProducto(Producto producto) { this.producto = producto; }
}
