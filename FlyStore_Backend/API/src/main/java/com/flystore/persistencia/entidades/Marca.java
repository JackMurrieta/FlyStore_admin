package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad Marca - Una marca tiene muchos productos
 */
@Entity
@Table(name = "marca")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column
    private Boolean activo;
}
