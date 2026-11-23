package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * CU-04 · Body de PATCH /api/admin/pedidos/{folio}/estado. Para ENVIADO se exige paquetería y guía,
 * o {@code entregaLocal = true}.
 */
public record ActualizarEstadoPeticionDTO(
        String estado,
        @Size(max = 100, message = "La paquetería es demasiado larga") String paqueteria,
        @Size(max = 100, message = "La guía es demasiado larga") String guia,
        @JsonProperty("entrega_local") boolean entregaLocal) {}
