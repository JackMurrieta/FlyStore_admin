package com.flystore.contratos.catalogo;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Alta de producto. Cada color de las variantes debe tener al menos una imagen en {@code imagenes}
 * (con archivo en multipart o con url en JSON).
 */
public record CrearProductoDTO(@NotBlank
        String clave, @NotBlank
        String nombre, @NotBlank
        String descripcion, @NotBlank
        String idCategoria, @NotBlank
        String idSubcategoria, @NotBlank
        String idMarca, @NotEmpty
        List<@NotNull @Valid ImagenColorDTO> imagenes, @NotBlank
        String estado, @NotEmpty
        List<@NotNull @Valid VarianteDTO> variantes, @NotEmpty
        List<@NotNull @Valid PrecioProductoDTO> preciosProducto) {}
