package com.flystore.contratos.catalogo;

import java.math.BigDecimal;

/**
 * Esquema de precio del producto. Al cambiar la cantidad en la ficha, el precio por pieza es el del
 * esquema con el mayor {@code minPiezas} que no supere la cantidad (igual que en el carrito).
 *
 * @param referencia true en el esquema del tipo de cliente (el que se muestra de entrada).
 */
public record NivelPrecioCatalogoDTO(String tipoPrecio, Integer minPiezas, BigDecimal precio, boolean referencia) {}
