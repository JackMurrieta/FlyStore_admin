package com.flystore.contratos.catalogo;

import java.math.BigDecimal;

/**
 * Precio que se muestra de entrada: el del esquema igual al tipo de cliente (Base sin sesión o si el
 * producto no tiene ese esquema).
 *
 * @param minPiezas piezas del producto para alcanzar ese precio en el carrito.
 */
public record PrecioReferenciaDTO(String tipoPrecio, BigDecimal precio, Integer minPiezas) {}
