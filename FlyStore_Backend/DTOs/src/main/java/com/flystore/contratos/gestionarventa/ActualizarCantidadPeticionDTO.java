package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Alt1 · Cambiar cantidad. Cantidad 0 quita la línea del carrito. */
public record ActualizarCantidadPeticionDTO(
        @NotNull(message = "Elige una variante") @Positive Long varianteProductoId,
        @NotNull(message = "Indica la cantidad") @PositiveOrZero(message = "La cantidad no puede ser negativa")
        @Max(value = 9999, message = "Cantidad demasiado grande") Integer cantidad) {}
