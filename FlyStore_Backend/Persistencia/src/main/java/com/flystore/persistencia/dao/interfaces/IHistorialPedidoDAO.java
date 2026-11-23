package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.HistorialPedido;
import java.util.List;

/** Historial de cambios de estado de un pedido (CU-04 Administrar pedidos). */
public interface IHistorialPedidoDAO {

    /** Guarda una entrada nueva del historial. */
    HistorialPedido guardar(HistorialPedido entrada);

    /** Entradas del pedido, de la más reciente a la más antigua. */
    List<HistorialPedido> listarPorPedido(Long pedidoId);
}
