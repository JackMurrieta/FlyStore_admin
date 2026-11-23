package com.flystore.contratos.catalogo;

import java.util.List;

/**
 * Producto en la cuadrícula del catálogo: foto, nombre, marca, precio de referencia y sus colores con
 * existencia (cada uno con su foto) para cambiar la imagen desde la tarjeta.
 *
 * Si se filtró por color, {@code idColorDestacado} es el color buscado que tiene el producto: la foto
 * es la de ese color y va primero en {@code colores}. Sin filtro de color es {@code null}.
 */
public record ProductoTarjetaDTO(
        Long idProducto,
        String clave,
        String nombre,
        String marca,
        String subcategoria,
        String imagenUrl,
        PrecioReferenciaDTO precio,
        List<ColorTarjetaDTO> colores,
        Long idColorDestacado) {}
