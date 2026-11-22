package com.flystore.persistencia.consultas;

/** Marca con productos visibles en el alcance, su logo y cuántos productos tiene. */
public record ConteoMarca(Long id, String nombre, String logoUrl, long cantidad) {}
