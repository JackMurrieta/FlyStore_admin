package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

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
}
