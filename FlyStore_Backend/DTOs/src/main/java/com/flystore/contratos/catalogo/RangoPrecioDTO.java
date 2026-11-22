package com.flystore.contratos.catalogo;

import java.math.BigDecimal;

/** Precio de referencia mínimo y máximo de la línea; null si no hay productos. */
public record RangoPrecioDTO(BigDecimal minimo, BigDecimal maximo) {}
