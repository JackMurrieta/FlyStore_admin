package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad CodigoDescuento - Codigos de descuento aplicables a productos
 */
@Entity
@Table(name = "codigo_descuento")
public class CodigoDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigo;

    @Column(name = "monto_fijo")
    private BigDecimal montoFijo;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "limite_usos")
    private Integer limiteUsos;

    @Column(name = "usos_actuales")
    private Integer usosActuales;

    @Column
    private Boolean activo;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;
}
