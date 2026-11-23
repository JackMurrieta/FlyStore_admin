package com.flystore.persistencia.consultas;

import com.flystore.persistencia.entidades.PedidoVenta;
import java.util.List;

/** Una página de pedidos y el total que cumple los filtros (CU-04). */
public record PaginaPedidos(List<PedidoVenta> pedidos, long total) {
    public PaginaPedidos {
        pedidos = pedidos == null ? List.of() : List.copyOf(pedidos);
    }
}
