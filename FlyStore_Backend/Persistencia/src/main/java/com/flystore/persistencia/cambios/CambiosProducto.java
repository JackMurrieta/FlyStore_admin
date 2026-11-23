package com.flystore.persistencia.cambios;

import com.flystore.persistencia.entidades.EstadoProducto;
import com.flystore.persistencia.entidades.PrecioProducto;
import com.flystore.persistencia.entidades.VarianteProducto;
import java.util.List;
import java.util.Map;

/**
 * Lo que guarda la edición de un producto (CU-03 S1), ya validado por Negocio. Lo que no viene se
 * quita: variantes que no están en {@code existencias}, imágenes que no están en {@code imagenes} y
 * todos los precios anteriores.
 *
 * @param existencias     variantes que se conservan (id → nueva existencia)
 * @param variantesNuevas variantes nuevas con talla y color por nombre (se resuelven contra el catálogo)
 * @param imagenes        galería en orden; cada imagen con el nombre de su color
 * @param precios         esquemas de precio que reemplazan a los anteriores
 */
public record CambiosProducto(
        String clave,
        String nombre,
        String descripcion,
        Long idSubcategoria,
        Long idMarca,
        EstadoProducto estado,
        Map<Long, Integer> existencias,
        List<VarianteProducto> variantesNuevas,
        List<ImagenCambio> imagenes,
        List<PrecioProducto> precios) {

    /** Imagen de la galería: {@code idImagen} si ya existe; si es nueva, {@code url}. */
    public record ImagenCambio(Long idImagen, String url, String color) {}
}
