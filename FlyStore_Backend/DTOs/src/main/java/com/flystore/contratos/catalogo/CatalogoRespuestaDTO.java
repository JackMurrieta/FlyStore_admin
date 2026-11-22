package com.flystore.contratos.catalogo;

import java.util.List;

/**
 * CU-01 · Página del catálogo de una línea o de la búsqueda en toda la tienda ({@code linea} null).
 *
 * @param productos            bloque de hasta {@code tamanoPagina} productos en el orden pedido.
 * @param total                productos que cumplen la búsqueda y los filtros.
 * @param hayMas               false cuando ya no hay más bloques (A6 Fin del catálogo).
 * @param tipoPrecioReferencia esquema con el que se calcularon los precios (el del tipo de cliente).
 * @param sugerencias          solo con búsqueda sin resultados (A2): coincidencias parciales de la línea
 *                             o, si no hay, sus más vendidos. Vacía en cualquier otro caso.
 * @param lineas               solo en la búsqueda de toda la tienda: resultados por línea para filtrar
 *                             («12 coincidencias en Gorras»). Vacía en el catálogo de una línea.
 */
public record CatalogoRespuestaDTO(
        LineaDTO linea,
        List<ProductoTarjetaDTO> productos,
        OpcionesFiltroRespuestaDTO opcionesFiltro,
        int pagina,
        int tamanoPagina,
        long total,
        boolean hayMas,
        String tipoPrecioReferencia,
        List<ProductoTarjetaDTO> sugerencias,
        List<LineaConteoDTO> lineas) {}
