package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad PagoEfectivo - Pago en efectivo
 */
@Entity
@Table(name = "pago_efectivo")
public class PagoEfectivo extends Pago {

    @Column(name = "cantidad_recibida")
    private BigDecimal cantidadRecibida;

    @Column
    private BigDecimal cambio;

    public BigDecimal getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(BigDecimal cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public void setCambio(BigDecimal cambio) {
        this.cambio = cambio;
    }
    
    
}
