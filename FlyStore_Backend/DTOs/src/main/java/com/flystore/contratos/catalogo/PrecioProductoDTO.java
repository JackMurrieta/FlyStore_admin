package com.flystore.contratos.catalogo;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PrecioProductoDTO(@NotBlank
        String tipoPrecio, @NotNull
        @DecimalMin("0.01")
        BigDecimal precio, @NotNull
        @Positive
        Integer minPiezas) {}
