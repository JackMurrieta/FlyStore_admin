package com.flystore.contratos.catalogo;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record VarianteDTO(@NotBlank
        String talla, @NotBlank
        String color, @NotNull
        @PositiveOrZero
        Integer stock) {}
