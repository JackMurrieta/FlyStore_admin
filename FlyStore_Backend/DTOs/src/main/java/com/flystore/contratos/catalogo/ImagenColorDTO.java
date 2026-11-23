package com.flystore.contratos.catalogo;

import jakarta.validation.constraints.*;

/**
 * Imagen del producto ligada a un color de sus variantes (alta y edición).
 * El orden de la lista es el orden de la galería: la primera imagen de cada color es la principal
 * de ese color y la primera de todas es la principal del producto.
 *
 * Se indica exactamente un origen:
 *   - archivo: posición del archivo en la parte multipart «imagenes» (0, 1, 2…);
 *   - url: imagen ya publicada (alta por JSON);
 *   - idImagen: imagen que ya tiene el producto y se conserva (solo al editar).
 */
public record ImagenColorDTO(
        String idImagen,
        String url,
        Integer archivo,
        @NotBlank String color) {}
