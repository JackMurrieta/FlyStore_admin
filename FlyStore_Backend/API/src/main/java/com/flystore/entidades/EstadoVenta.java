package com.flystore.entidades;

/**
 * Enum EstadoVenta - Estados del pedido de venta
 */
public enum EstadoVenta {
    PENDIENTE,
    APARTADO_PENDIENTE_PAGO,
    PENDIENTE_COMPROBANTE,
    PAGO_EN_REVISION,
    PAGADO,
    PREPARACION,
    ENVIADO,
    ENTREGADO,
    CANCELADO,
    CANCELADO_VENCIMIENTO
}
