package com.flystore.contratos.catalogo;

/** Marca de la línea con su logo y cuántos productos visibles tiene (mosaico de marcas y filtro). */
public record MarcaFiltroDTO(Long id, String nombre, String logoUrl, long cantidad) {}
