package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad PagoTransferencia - Pago por transferencia bancaria
 */
@Entity
@Table(name = "pago_transferencia")
public class PagoTransferencia extends Pago {

    @Column(name = "comprobante_pago_url")
    private String comprobantePagoUrl;

    @Column(name = "numero_referencia")
    private String numeroReferencia;

    /** Momento en que llegó el comprobante; ordena la revisión de pagos (FIFO). */
    @Column(name = "fecha_hora_comprobante")
    private OffsetDateTime fechaHoraComprobante;

    public OffsetDateTime getFechaHoraComprobante() {
        return fechaHoraComprobante;
    }

    public void setFechaHoraComprobante(OffsetDateTime fechaHoraComprobante) {
        this.fechaHoraComprobante = fechaHoraComprobante;
    }

    public String getComprobantePagoUrl() {
        return comprobantePagoUrl;
    }

    public void setComprobantePagoUrl(String comprobantePagoUrl) {
        this.comprobantePagoUrl = comprobantePagoUrl;
    }

    public String getNumeroReferencia() {
        return numeroReferencia;
    }

    public void setNumeroReferencia(String numeroReferencia) {
        this.numeroReferencia = numeroReferencia;
    }
    
    
}
