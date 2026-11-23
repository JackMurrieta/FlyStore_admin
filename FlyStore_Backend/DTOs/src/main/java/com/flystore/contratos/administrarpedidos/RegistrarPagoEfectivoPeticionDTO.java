package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/** CU-04 · Body de POST /api/admin/pedidos/{folio}/pago-efectivo. */
public record RegistrarPagoEfectivoPeticionDTO(
        @JsonProperty("monto_recibido")
        @Digits(integer = 10, fraction = 2, message = "Monto inválido") BigDecimal montoRecibido) {}
