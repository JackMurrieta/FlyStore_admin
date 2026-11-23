package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;

/** CU-04 · Envío del pedido: paquetería y guía, o entrega local sin ellas. */
public record DatosEnvioDTO(
        String paqueteria,
        String guia,
        @JsonProperty("entrega_local") boolean entregaLocal) {}
