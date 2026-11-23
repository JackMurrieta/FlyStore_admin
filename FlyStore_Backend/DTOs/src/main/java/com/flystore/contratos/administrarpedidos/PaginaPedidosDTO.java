package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** CU-04 · Respuesta de GET /api/admin/pedidos: la página pedida y cuántos pedidos cumplen los filtros. */
public record PaginaPedidosDTO(
        List<PedidoResumenDTO> pedidos,
        @JsonProperty("total_encontrado") long totalEncontrado) {}
