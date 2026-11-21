package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/**
 * El personal confirma que el pedido ya está pagado (comprobante revisado o efectivo recibido).
 *
 * @param cantidadRecibida obligatoria en pago en efectivo; se ignora en transferencia.
 */
public record ConfirmarPagoPeticionDTO(
        @DecimalMin(value = "0.00", message = "La cantidad recibida no puede ser negativa")
        @Digits(integer = 10, fraction = 2, message = "Cantidad inválida") BigDecimal cantidadRecibida) {}
