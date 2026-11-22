package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Forma de pago elegida. El archivo del comprobante nunca viaja aquí: se sube antes con
 * el endpoint de comprobante y se envía la ruta que devolvió.
 *
 * <p>Al registrar, el front envía {@code tipoPago}, {@code folio} y, si subió comprobante,
 * {@code comprobanteRuta}. Los últimos tres campos solo los llena el backend en las respuestas:
 * hora de llegada del comprobante y, en efectivo, lo recibido y el cambio.
 */
public record MetodoPagoDTO(
        @NotBlank(message = "Elige una forma de pago")
        @Pattern(regexp = "TRANSFERENCIA|EFECTIVO", message = "Forma de pago inválida") String tipoPago,
        @NotBlank(message = "Falta el folio del pedido") @Size(max = 30) String folio,
        @Size(max = 500) String comprobanteRuta,
        @Size(max = 255) String comprobanteNombre,
        OffsetDateTime fechaHoraComprobante,
        BigDecimal cantidadRecibida,
        BigDecimal cambio) {

    /** Forma de pago tal como la envía el front al registrar. */
    public MetodoPagoDTO(String tipoPago, String folio, String comprobanteRuta, String comprobanteNombre) {
        this(tipoPago, folio, comprobanteRuta, comprobanteNombre, null, null, null);
    }
}
