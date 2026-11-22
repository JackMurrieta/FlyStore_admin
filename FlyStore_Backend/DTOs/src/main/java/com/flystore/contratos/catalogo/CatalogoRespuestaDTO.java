package com.flystore.contratos.catalogo;

import java.util.List;

/**
 * CU-01 · Página del catálogo de una línea.
 *
 * @param productos            bloque de hasta {@code tamanoPagina} productos en el orden pedido.
 * @param total                productos que cumplen la búsqueda y los filtros.
 * @param hayMas               false cuando ya no hay más bloques (A6 Fin del catálogo).
 * @param tipoPrecioReferencia esquema con el que se calcularon los precios (el del tipo de cliente).
 * @param sugerencias          solo con búsqueda sin resultados (A2): coincidencias parciales de la línea
 *                             o, si no hay, sus más vendidos. Vacía en cualquier otro caso.
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
        List<ProductoTarjetaDTO> sugerencias) {}
