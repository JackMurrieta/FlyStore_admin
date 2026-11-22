package com.flystore.contratos.catalogo;

import java.util.List;

/**
 * Todo lo necesario para dibujar los filtros de una línea. Se calcula sobre los productos visibles de
 * la línea (sin aplicar los filtros elegidos), así las opciones no desaparecen al marcar una.
 *
 * @param unidadesMedida valores para el parámetro unidadMedida (ej. «in», «cm»).
 * @param ordenes        valores para el parámetro orden con su etiqueta para la vista.
 */
public record OpcionesFiltroRespuestaDTO(
        List<OpcionFiltroDTO> marcas,
        List<OpcionFiltroDTO> subcategorias,
        List<ColorFiltroDTO> colores,
        List<TipoTallaFiltroDTO> tiposTalla,
        List<String> unidadesMedida,
        RangoPrecioDTO rangoPrecio,
        List<CatalogoItemDTO> ordenes) {}
