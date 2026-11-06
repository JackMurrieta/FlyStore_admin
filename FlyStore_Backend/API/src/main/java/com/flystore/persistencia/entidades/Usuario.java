package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad Usuario - Clase base para Admin, Cliente y Empleado
 * Usa herencia JOINED
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_id", nullable = false, unique = true)
    private UUID authId;

    @Column
    private String nombre;

    @Column
    private String apellidos;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column
    private String telefono;

    @Column
    private Boolean activo;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;
}
