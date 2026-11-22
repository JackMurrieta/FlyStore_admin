package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.util.List;

/**
 * Entidad Categoria - Una categoria tiene muchas subcategorias y tipos de talla
 */
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String nombre;

    @Column(name = "logo_url")
    private String logoUrl;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subcategoria> subcategorias;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TipoTalla> tiposTalla;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public List<Subcategoria> getSubcategorias() {
        return subcategorias;
    }

    public void setSubcategorias(List<Subcategoria> subcategorias) {
        this.subcategorias = subcategorias;
    }

    public List<TipoTalla> getTiposTalla() {
        return tiposTalla;
    }

    public void setTiposTalla(List<TipoTalla> tiposTalla) {
        this.tiposTalla = tiposTalla;
    }
    
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
