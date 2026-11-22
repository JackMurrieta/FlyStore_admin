package com.flystore.contratos.catalogo;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CrearProductoDTO(@NotBlank
        String clave, @NotBlank
        String nombre, @NotBlank
        String descripcion, @NotBlank
        String idCategoria, @NotBlank
        String idSubcategoria, @NotBlank
        String idMarca, @NotEmpty
        List<@NotBlank String> imagenes, @NotBlank
        String estado, @NotEmpty
        List<@NotNull @Valid VarianteDTO> variantes, @NotEmpty
        List<@NotNull @Valid PrecioProductoDTO> preciosProducto) {}
