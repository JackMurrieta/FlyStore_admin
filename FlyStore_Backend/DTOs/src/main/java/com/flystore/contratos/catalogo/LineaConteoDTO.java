package com.flystore.contratos.catalogo;

/** Línea con el número de productos que encontró la búsqueda en toda la tienda. */
public record LineaConteoDTO(Long id, String nombre, String ruta, long cantidad) {}
