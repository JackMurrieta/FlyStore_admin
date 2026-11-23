package com.flystore.contratos.catalogo;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * Edición de un producto (CU-03 S1). La línea (categoría) no cambia: el tipo debe ser de la misma línea.
 * Lo que no se manda se quita: variantes (si no tienen pedidos), imágenes y precios.
 */
public record EditarProductoDTO(
        @NotBlank String clave,
        @NotBlank String nombre,
        @NotBlank String descripcion,
        @NotBlank String idSubcategoria,
        @NotBlank String idMarca,
        @NotBlank String estado,
        @NotEmpty List<@NotNull @Valid VarianteEdicionDTO> variantes,
        @NotEmpty List<@NotNull @Valid PrecioProductoDTO> preciosProducto,
        @NotEmpty List<@NotNull @Valid ImagenColorDTO> imagenes) {}
