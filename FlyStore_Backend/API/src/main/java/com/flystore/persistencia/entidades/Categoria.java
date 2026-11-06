package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

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
}
