package com.flystore.contratos.catalogo;

/**
 * Color con existencia de un producto, para los cuadritos de la tarjeta: al pasar el mouse (o tocarlo
 * en el celular) la foto de la tarjeta cambia a {@code imagenUrl}.
 *
 * @param imagenUrl primera foto de ese color; null si el color no tiene foto propia.
 */
public record ColorTarjetaDTO(Long idColor, String nombre, String hex, String imagenUrl) {}
