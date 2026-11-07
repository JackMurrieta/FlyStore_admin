package com.flystore.entidades;

import jakarta.persistence.*;
import java.util.List;

/**
 * Entidad Talla - Tallas disponibles asociadas a un tipo de talla
 */
@Entity
@Table(name = "talla")
public class Talla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_talla_id", nullable = false)
    private TipoTalla tipoTalla;

    @Column
    private String etiqueta;

    @OneToMany(mappedBy = "talla")
    private List<VarianteProducto> variantesProducto;

    public Long getId() {
        return id;
    }

    public TipoTalla getTipoTalla() {
        return tipoTalla;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public List<VarianteProducto> getVariantesProducto() {
        return variantesProducto;
    }

    public void setVariantesProducto(List<VarianteProducto> variantesProducto) {
        this.variantesProducto = variantesProducto;
    }
    
    
}
