package com.flystore.persistencia.entidades;

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
}
