package com.flystore.contratos.catalogo;

import jakarta.validation.constraints.*;

/**
 * Variante al editar un producto.
 *   - Con {@code idVariante}: variante que ya existe; solo cambia su existencia (talla y color no se
 *     cambian: para eso se quita y se agrega otra).
 *   - Sin {@code idVariante}: variante nueva con talla y color del catálogo.
 */
public record VarianteEdicionDTO(
        String idVariante,
        String talla,
        String color,
        @NotNull @PositiveOrZero Integer stock) {}
