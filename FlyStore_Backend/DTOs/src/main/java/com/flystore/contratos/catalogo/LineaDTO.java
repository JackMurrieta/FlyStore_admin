package com.flystore.contratos.catalogo;

/**
 * Línea de producto (categoría) del catálogo de la tienda.
 *
 * @param ruta segmento de la URL de la línea: /api/catalogo/{ruta} (caps, essence, clothes, shoes).
 */
public record LineaDTO(Long id, String nombre, String ruta, String logoUrl) {}
