package com.flystore.contratos.gestionarventa;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * PedidoVenta que recibe el frontend. El carrito es este mismo DTO en estado PENDIENTE.
 * Los importes los calcula el backend; el front solo los muestra.
 *
 * <p>{@code tipoPrecioAplicado} es el esquema común a todas las líneas, o "MIXTO" si los productos
 * alcanzaron esquemas distintos. {@code siguienteTipoPrecio}, {@code piezasParaSiguienteNivel} y
 * {@code nivelesPrecio} solo se informan cuando el carrito tiene un único producto, porque el precio
 * se calcula por producto. {@code fechaLimitePago} es el vencimiento (42 h) de un pedido registrado
 * sin pago; es null en los demás estados.
 */
public record PedidoVentaDTO(
        Long id,
        String folio,
        String estadoVenta,
        List<DetallePedidoDTO> detalles,
        int totalPiezas,
        String tipoPrecioAplicado,
        String siguienteTipoPrecio,
        Integer piezasParaSiguienteNivel,
        List<NivelPrecioDTO> nivelesPrecio,
        BigDecimal subtotalPrecioBase,
        BigDecimal ahorroPorTipoPrecio,
        BigDecimal totalDescuentos,
        BigDecimal costoEnvio,
        BigDecimal total,
        DatosEntregaDTO datosEntrega,
        MetodoPagoDTO metodoPago,
        OffsetDateTime fechaHoraPedido,
        OffsetDateTime fechaLimitePago,
        String enlaceWhatsapp) {}
