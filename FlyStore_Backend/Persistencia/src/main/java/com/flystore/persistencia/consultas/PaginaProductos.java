package com.flystore.persistencia.consultas;

import java.util.List;

/** Identificadores de una página de productos en el orden pedido y el total que cumple el filtro. */
public record PaginaProductos(List<Long> ids, long total) {
    public PaginaProductos {
        ids = ids == null ? List.of() : List.copyOf(ids);
    }
}
