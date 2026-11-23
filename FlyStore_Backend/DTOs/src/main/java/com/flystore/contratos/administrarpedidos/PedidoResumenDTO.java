package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * CU-04 · Renglón de la lista de pedidos del personal. Las fechas viajan en ISO-8601 con zona.
 * {@code fechaLimite} es el vencimiento de un pedido sin pago; null en los demás estados.
 */
public record PedidoResumenDTO(
        String folio,
        @JsonProperty("fecha_creacion") String fechaCreacion,
        @JsonProperty("cliente_nombre") String clienteNombre,
        @JsonProperty("cliente_telefono") String clienteTelefono,
        BigDecimal total,
        @JsonProperty("metodo_pago") String metodoPago,
        String estado,
        @JsonProperty("fecha_limite") String fechaLimite) {}
