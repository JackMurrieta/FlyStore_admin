package com.flystore.contratos.catalogo;

import java.util.List;

/**
 * Producto en la cuadrícula del catálogo: foto, nombre, marca, precio de referencia y sus colores con
 * existencia (cada uno con su foto) para cambiar la imagen desde la tarjeta.
 */
public record ProductoTarjetaDTO(
        Long idProducto,
        String clave,
        String nombre,
        String marca,
        String subcategoria,
        String imagenUrl,
        PrecioReferenciaDTO precio,
        List<ColorTarjetaDTO> colores) {}
