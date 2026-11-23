package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * CU-04 · Línea del pedido. {@code precioUnitario} ya incluye el descuento por pieza del código
 * aplicado, de modo que subtotal = precioUnitario × cantidad.
 */
public record LineaPedidoDTO(
        String producto,
        String talla,
        int cantidad,
        @JsonProperty("precio_unitario") BigDecimal precioUnitario,
        BigDecimal subtotal) {}
