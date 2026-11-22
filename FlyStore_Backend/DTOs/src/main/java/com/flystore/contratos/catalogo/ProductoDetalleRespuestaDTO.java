package com.flystore.contratos.catalogo;

import java.util.List;

/**
 * CU-01 · 3.2.4 Ficha del producto.
 *
 * @param variantes        todas las variantes, también las agotadas (disponible = false).
 * @param precios          esquemas ordenados por mínimo de piezas.
 * @param precioReferencia esquema del tipo de cliente (Base sin sesión).
 * @param cantidadInicial  piezas con las que empieza el selector: el mínimo del esquema de referencia,
 *                         para que el precio mostrado coincida con el del carrito.
 * @param existenciaTotal  piezas disponibles sumando todas las variantes.
 */
public record ProductoDetalleRespuestaDTO(
        Long idProducto,
        String clave,
        String nombre,
        String descripcion,
        String estado,
        LineaDTO linea,
        ElementoCatalogoDTO marca,
        ElementoCatalogoDTO subcategoria,
        List<ImagenProductoDTO> imagenes,
        List<VarianteVentaDTO> variantes,
        List<NivelPrecioCatalogoDTO> precios,
        PrecioReferenciaDTO precioReferencia,
        int cantidadInicial,
        int existenciaTotal,
        boolean disponible) {}
