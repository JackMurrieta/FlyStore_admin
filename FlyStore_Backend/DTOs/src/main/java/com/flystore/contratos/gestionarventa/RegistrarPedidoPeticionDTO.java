package com.flystore.contratos.gestionarventa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Petición para registrar el pedido en curso. El front envía el PedidoVentaDTO completo;
 * el backend solo toma estos campos y recalcula todo lo demás.
 *
 * @param total total que vio el cliente; si los precios cambiaron, el registro se rechaza.
 */
public record RegistrarPedidoPeticionDTO(
        @NotNull(message = "Faltan los datos de entrega") @Valid DatosEntregaDTO datosEntrega,
        @NotNull(message = "Falta la forma de pago") @Valid MetodoPagoDTO metodoPago,
        BigDecimal total) {}
