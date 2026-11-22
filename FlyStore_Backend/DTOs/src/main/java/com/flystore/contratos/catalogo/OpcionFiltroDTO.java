package com.flystore.contratos.catalogo;

/** Opción de un filtro (marca, subcategoría o talla) y cuántos productos visibles de la línea la tienen. */
public record OpcionFiltroDTO(Long id, String nombre, long cantidad) {}
