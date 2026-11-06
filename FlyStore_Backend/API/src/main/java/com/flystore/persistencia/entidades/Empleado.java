package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidad Empleado - Usuario empleado con fecha de contratación
 */
@Entity
@Table(name = "empleado")
public class Empleado extends Usuario {

    @Column(name = "fecha_contratacion")
    private LocalDate fechaContratacion;

    public LocalDate getFechaContratacion() {
        return fechaContratacion;
    }

    public void setFechaContratacion(LocalDate fechaContratacion) {
        this.fechaContratacion = fechaContratacion;
    }
    
    
}
