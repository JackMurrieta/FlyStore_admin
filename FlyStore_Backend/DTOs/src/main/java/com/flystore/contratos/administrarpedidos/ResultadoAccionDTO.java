package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CU-04 · Respuesta de las acciones sobre un pedido: cómo quedó y si se pudo avisar al cliente.
 * Con {@code notificacionEnviada = false} el frontend ofrece avisar a mano por WhatsApp.
 */
public record ResultadoAccionDTO(
        DetallePedidoAdminDTO pedido,
        @JsonProperty("notificacion_enviada") boolean notificacionEnviada) {}
