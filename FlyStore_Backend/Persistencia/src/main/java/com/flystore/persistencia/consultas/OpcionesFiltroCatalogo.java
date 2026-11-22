package com.flystore.persistencia.consultas;

import java.math.BigDecimal;
import java.util.List;

/**
 * Opciones para llenar los filtros de una línea: lo que existe entre sus productos visibles,
 * con cuántos productos tiene cada opción y el rango del precio de referencia.
 */
public record OpcionesFiltroCatalogo(
        List<ConteoMarca> marcas,
        List<ConteoOpcion> subcategorias,
        List<ConteoColor> colores,
        List<ConteoTalla> tallas,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo) {

    public OpcionesFiltroCatalogo {
        marcas = marcas == null ? List.of() : List.copyOf(marcas);
        subcategorias = subcategorias == null ? List.of() : List.copyOf(subcategorias);
        colores = colores == null ? List.of() : List.copyOf(colores);
        tallas = tallas == null ? List.of() : List.copyOf(tallas);
    }
}
