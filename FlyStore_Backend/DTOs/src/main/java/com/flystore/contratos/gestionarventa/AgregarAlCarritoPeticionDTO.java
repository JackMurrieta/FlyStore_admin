package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Agrega piezas de una variante al pedido en curso; si ya estaba, se suman. */
public record AgregarAlCarritoPeticionDTO(
        @NotNull(message = "Elige una variante") @Positive Long varianteProductoId,
        @NotNull(message = "Indica la cantidad") @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 9999, message = "Cantidad demasiado grande") Integer cantidad) {}
