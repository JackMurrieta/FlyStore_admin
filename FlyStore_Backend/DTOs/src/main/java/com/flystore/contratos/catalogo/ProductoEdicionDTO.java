package com.flystore.contratos.catalogo;

import java.util.List;

/** Producto completo para el formulario de edición (GET) y respuesta al guardar (PUT). */
public record ProductoEdicionDTO(
        String idProducto,
        String clave,
        String nombre,
        String descripcion,
        String idCategoria,
        String categoria,
        String idSubcategoria,
        String subcategoria,
        String idMarca,
        String marca,
        String estado,
        List<VarianteAdminDTO> variantes,
        List<PrecioProductoDTO> preciosProducto,
        List<ImagenAdminDTO> imagenes) {}
