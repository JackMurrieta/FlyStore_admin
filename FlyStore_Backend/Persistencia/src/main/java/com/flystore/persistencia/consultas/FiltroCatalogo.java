package com.flystore.persistencia.consultas;

import com.flystore.persistencia.entidades.TipoPrecio;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Criterios de una consulta del catálogo de la tienda. La misma consulta sirve para cada línea
 * (CAPS, ESSENCE, CLOTHES, SHOES), para la búsqueda por texto y para llenar los filtros.
 *
 * <p>Solo se consideran productos visibles: PUBLICADO o STOCK_BAJO, con al menos una variante con
 * existencia y con precio. Un conjunto vacío o un valor null no filtra.
 *
 * @param idCategoria       línea del catálogo; null busca en toda la tienda.
 * @param terminos          palabras ya normalizadas (minúsculas y sin acentos).
 * @param cualquierTermino  true basta con una palabra (sugerencias); false deben estar todas.
 * @param tallas            tallas; se combinan con colores y unidades sobre la misma variante con existencia.
 * @param tipoPrecio        esquema de referencia para el precio mostrado, el rango y el orden por precio.
 */
public record FiltroCatalogo(
        Long idCategoria,
        List<String> terminos,
        boolean cualquierTermino,
        Set<Long> marcas,
        Set<Long> subcategorias,
        Set<Long> tallas,
        Set<Long> colores,
        Set<String> unidadesMedida,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo,
        TipoPrecio tipoPrecio,
        OrdenCatalogo orden) {

    public FiltroCatalogo {
        terminos = terminos == null ? List.of() : List.copyOf(terminos);
        marcas = copia(marcas);
        subcategorias = copia(subcategorias);
        tallas = copia(tallas);
        colores = copia(colores);
        unidadesMedida = copia(unidadesMedida);
        tipoPrecio = tipoPrecio == null ? TipoPrecio.BASE : tipoPrecio;
        orden = orden == null ? OrdenCatalogo.RECIENTES : orden;
    }

    /** Todos los productos visibles de una línea, sin texto ni filtros. */
    public static FiltroCatalogo deLinea(Long idCategoria, TipoPrecio tipoPrecio) {
        return new FiltroCatalogo(idCategoria, List.of(), false, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                null, null, tipoPrecio, OrdenCatalogo.RECIENTES);
    }

    /** La línea sin texto ni filtros: es el alcance con el que se llenan las opciones de los filtros. */
    public FiltroCatalogo soloLinea() {
        return deLinea(idCategoria, tipoPrecio).conOrden(orden);
    }

    /** Coincidencias parciales: misma línea y basta con una de las palabras buscadas. */
    public FiltroCatalogo coincidenciaParcial() {
        return new FiltroCatalogo(idCategoria, terminos, true, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                null, null, tipoPrecio, OrdenCatalogo.MAS_VENDIDOS);
    }

    public FiltroCatalogo conOrden(OrdenCatalogo nuevoOrden) {
        return new FiltroCatalogo(idCategoria, terminos, cualquierTermino, marcas, subcategorias, tallas, colores,
                unidadesMedida, precioMinimo, precioMaximo, tipoPrecio, nuevoOrden);
    }

    public boolean tieneTexto() {
        return !terminos.isEmpty();
    }

    /** true si filtra por talla, color o unidad de medida, que se revisan sobre las variantes. */
    public boolean filtraVariantes() {
        return !tallas.isEmpty() || !colores.isEmpty() || !unidadesMedida.isEmpty();
    }

    private static <T> Set<T> copia(Set<T> valores) {
        if (valores == null) return Set.of();
        var copia = new LinkedHashSet<T>();
        valores.stream().filter(java.util.Objects::nonNull).forEach(copia::add);
        return java.util.Collections.unmodifiableSet(copia);
    }
}
