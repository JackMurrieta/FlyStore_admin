package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.util.List;

/**
 * Entidad Color - Colores disponibles para productos
 */
@Entity
@Table(name = "color")
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_color")
    private String nombreColor;

    @Column
    private String hex;

    @OneToMany(mappedBy = "color")
    private List<ImagenProducto> imagenesProducto;

    @OneToMany(mappedBy = "color")
    private List<VarianteProducto> variantesProducto;

    public String getNombreColor() {
        return nombreColor;
    }

    public void setNombreColor(String nombreColor) {
        this.nombreColor = nombreColor;
    }

    public String getHex() {
        return hex;
    }

    public void setHex(String hex) {
        this.hex = hex;
    }

    public List<ImagenProducto> getImagenesProducto() {
        return imagenesProducto;
    }

    public void setImagenesProducto(List<ImagenProducto> imagenesProducto) {
        this.imagenesProducto = imagenesProducto;
    }

    public List<VarianteProducto> getVariantesProducto() {
        return variantesProducto;
    }

    public void setVariantesProducto(List<VarianteProducto> variantesProducto) {
        this.variantesProducto = variantesProducto;
    }
    
    
}
