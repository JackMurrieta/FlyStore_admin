package com.flystore.persistencia.consultas;

/** Talla con existencia, su tipo de talla y el número de productos visibles que la tienen. */
public record ConteoTalla(Long id, String etiqueta, Long idTipoTalla, String tipoTalla, String unidadMedida, long cantidad) {}
