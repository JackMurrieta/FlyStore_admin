package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.persistencia.entidades.EstadoVenta;
import com.flystore.persistencia.entidades.HistorialPedido;
import com.flystore.persistencia.entidades.PedidoVenta;
import java.util.List;
import java.util.UUID;

/** Historial de cambios de estado de un pedido (CU-04 Administrar pedidos). */
public interface IHistorialPedidoBO {

    /** Nombre con el que queda una acción cuando no se sabe quién la hizo. */
    String USUARIO_DESCONOCIDO = "Personal de FlyStore";

    /**
     * Deja constancia de un cambio de estado ya aplicado al pedido: quién, cuándo, de qué estado a
     * cuál y por qué. Debe llamarse dentro de la transacción del cambio.
     *
     * @param usuarioAuthId identificador de la sesión de quien hizo el cambio; con null, o si no es
     *                      un administrador ni un empleado, queda como {@link #USUARIO_DESCONOCIDO}.
     * @param motivo        null si la acción no lo pide.
     */
    void registrarCambio(PedidoVenta pedido, EstadoVenta anterior, UUID usuarioAuthId, String motivo, boolean comprobanteRechazado);

    /**
     * Deja constancia de un cambio que hizo el propio cliente (cancelar su pedido). Queda a nombre
     * del cliente del pedido, marcado como «(cliente)».
     */
    void registrarCambioDelCliente(PedidoVenta pedido, EstadoVenta anterior, String motivo);

    /** Entradas del pedido, de la más reciente a la más antigua. */
    List<HistorialPedido> listarPorPedido(Long pedidoId);
}
