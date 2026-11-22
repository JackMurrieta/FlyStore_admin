package com.flystore.persistencia.consultas;

/** Opción de un filtro (marca o subcategoría) con los productos visibles que la tienen. */
public record ConteoOpcion(Long id, String nombre, long cantidad) {}
