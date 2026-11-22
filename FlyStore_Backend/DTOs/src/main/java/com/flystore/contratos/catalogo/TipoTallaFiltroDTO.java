package com.flystore.contratos.catalogo;

import java.util.List;

/** Tipo de talla de la línea (ej. Numérica en «in») con sus tallas disponibles, ya ordenadas. */
public record TipoTallaFiltroDTO(Long id, String nombre, String unidadMedida, List<OpcionFiltroDTO> tallas) {}
