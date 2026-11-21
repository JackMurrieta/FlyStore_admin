package com.flystore.contratos.gestionarventa;

import java.math.BigDecimal;

/**
 * Línea del pedido: una variante de producto con su cantidad y el precio aplicado.
 *
 * @param tipoTalla         nombre del TipoTalla de la categoría («Talla», «Presentación»…).
 * @param existencia        stock actual de la variante; el carrito no lo descuenta.
 * @param precioBase        precio BASE del producto por pieza.
 * @param precioUnitario    precio por pieza del esquema alcanzado por el producto.
 * @param descuentoUnitario descuento por pieza del código aplicado (0 si no hay).
 * @param subtotal          (precioUnitario − descuentoUnitario) × cantidad.
 */
public record DetallePedidoDTO(
        Long id,
        Long varianteProductoId,
        String productoNombre,
        String marca,
        String tipoTalla,
        String talla,
        String color,
        String imagenUrl,
        int cantidad,
        Integer existencia,
        String tipoPrecio,
        BigDecimal precioBase,
        BigDecimal precioUnitario,
        BigDecimal descuentoUnitario,
        CodigoDescuentoAplicadoDTO codigoDescuento,
        BigDecimal subtotal) {}
