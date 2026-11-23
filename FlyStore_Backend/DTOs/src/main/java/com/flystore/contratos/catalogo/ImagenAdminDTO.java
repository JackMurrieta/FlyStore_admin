package com.flystore.contratos.catalogo;

/** Imagen en el formulario de edición, con el color al que está ligada ({@code null} = general). */
public record ImagenAdminDTO(
        String idImagen,
        String url,
        String idColor,
        String color,
        Integer orden) {}
