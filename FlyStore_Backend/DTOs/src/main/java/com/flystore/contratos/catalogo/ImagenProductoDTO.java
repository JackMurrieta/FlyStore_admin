package com.flystore.contratos.catalogo;

/** Imagen de la galería. {@code idColor} null = imagen general del producto. */
public record ImagenProductoDTO(String url, Long idColor, Integer orden) {}
