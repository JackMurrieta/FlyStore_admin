package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * CU-04 · Respuesta de GET /api/admin/pedidos/{folio}. {@code comprobante} solo existe en pagos por
 * transferencia que ya lo subieron; {@code envio}, desde que el pedido se marca como enviado.
 * El historial va del cambio más reciente al más antiguo.
 */
public record DetallePedidoAdminDTO(
        String folio,
        String estado,
        @JsonProperty("fecha_creacion") String fechaCreacion,
        @JsonProperty("fecha_limite") String fechaLimite,
        ClienteDTO cliente,
        @JsonProperty("datos_entrega") String datosEntrega,
        List<LineaPedidoDTO> lineas,
        BigDecimal total,
        @JsonProperty("metodo_pago") String metodoPago,
        ComprobanteDTO comprobante,
        DatosEnvioDTO envio,
        List<HistorialEntradaDTO> historial) {}
