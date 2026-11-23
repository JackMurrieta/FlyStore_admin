package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;

/** CU-04 · Un cambio de estado del pedido: quién, cuándo, de qué estado a cuál y por qué. */
public record HistorialEntradaDTO(
        @JsonProperty("fecha_hora") String fechaHora,
        String usuario,
        @JsonProperty("estado_anterior") String estadoAnterior,
        @JsonProperty("estado_nuevo") String estadoNuevo,
        String motivo) {}
