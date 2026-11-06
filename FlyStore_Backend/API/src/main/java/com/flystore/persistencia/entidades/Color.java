package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

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
}
