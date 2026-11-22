package com.flystore.contratos.catalogo;

/** Color con existencia en la línea y cuántos productos lo tienen. */
public record ColorFiltroDTO(Long id, String nombre, String hex, long cantidad) {}
