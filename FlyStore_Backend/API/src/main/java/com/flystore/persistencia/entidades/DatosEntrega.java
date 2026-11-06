package com.flystore.persistencia.entidades;

import jakarta.persistence.*;

/**
 * Entidad DatosEntrega - Direcciones de entrega de un cliente
 */
@Entity
@Table(name = "datos_entrega")
public class DatosEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column
    private String telefono;

    @Column
    private String ciudad;

    @Column
    private String estado;

    @Column
    private String direccion;

    @Column
    private String referencias;

    @Column
    private Boolean activo;
}
