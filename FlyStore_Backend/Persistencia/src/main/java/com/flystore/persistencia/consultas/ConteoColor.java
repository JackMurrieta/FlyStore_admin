package com.flystore.persistencia.consultas;

/** Color con existencia y el número de productos visibles que lo tienen. */
public record ConteoColor(Long id, String nombre, String hex, long cantidad) {}
