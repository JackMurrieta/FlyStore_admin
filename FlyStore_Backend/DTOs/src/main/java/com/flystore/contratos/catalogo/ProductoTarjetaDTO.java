package com.flystore.contratos.catalogo;

/** Producto en la cuadrícula del catálogo: foto, nombre, marca y precio de referencia. */
public record ProductoTarjetaDTO(
        Long idProducto,
        String clave,
        String nombre,
        String marca,
        String subcategoria,
        String imagenUrl,
        PrecioReferenciaDTO precio) {}
