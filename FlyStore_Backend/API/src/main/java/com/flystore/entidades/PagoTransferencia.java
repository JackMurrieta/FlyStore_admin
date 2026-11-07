package com.flystore.entidades;

import jakarta.persistence.*;

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
