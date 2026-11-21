package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alt2 · Canjear código de descuento. El backend lo aplica a los productos ligados al código. */
public record AplicarCodigoPeticionDTO(
        @NotBlank(message = "Escribe un código") @Size(max = 50, message = "Código demasiado largo") String codigo) {}
