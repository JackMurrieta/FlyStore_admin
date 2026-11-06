package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad DatoBancario - Datos bancarios de administradores
 */
@Entity
@Table(name = "dato_bancario")
public class DatoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "nombre_banco")
    private String nombreBanco;

    @Column
    private String titular;

    @Column(name = "numero_cuenta")
    private String numeroCuenta;

    @Column
    private String clabe;

    @Column
    private Boolean activo;
}
