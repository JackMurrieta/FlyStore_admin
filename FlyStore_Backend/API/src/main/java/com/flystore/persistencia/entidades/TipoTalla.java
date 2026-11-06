package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad TipoTalla - Tipos de talla asociados a categorías
 */
@Entity
@Table(name = "tipo_talla")
public class TipoTalla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column
    private String nombre;

    @Column(name = "unidad_medida")
    private String unidadMedida;
}
