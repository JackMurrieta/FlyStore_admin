package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.consultas.CriteriosBusquedaPedidos;
import com.flystore.persistencia.consultas.PaginaPedidos;
import com.flystore.persistencia.entidades.EstadoVenta;
import com.flystore.persistencia.entidades.Pago;
import com.flystore.persistencia.entidades.PedidoVenta;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Consultas y escrituras de PedidoVenta. El carrito es el pedido del cliente en estado PENDIENTE;
 * la base garantiza uno por cliente con el índice uk_pedido_venta_pendiente_por_cliente.
 */
public interface IPedidoVentaDAO {

    /** Crea el pedido PENDIENTE (carrito) del cliente. Falla si ya existe uno. */
    PedidoVenta crearPedidoVenta(Long clienteId);

    /** Pedido PENDIENTE del cliente, sin bloquearlo. */
    Optional<PedidoVenta> obtenerPedidoPendienteCliente(Long clienteId);

    /**
     * Pedido PENDIENTE del cliente con bloqueo de escritura hasta el fin de la transacción,
     * para que dos peticiones del mismo cliente no modifiquen el carrito a la vez.
     */
    Optional<PedidoVenta> bloquearPedidoPendienteCliente(Long clienteId);

    /** Pedido por id con bloqueo de escritura (para confirmar su pago). */
    Optional<PedidoVenta> bloquearPorId(Long idPedido);

    /** Pedido por folio con bloqueo de escritura (para subir su comprobante). */
    Optional<PedidoVenta> bloquearPorFolio(String folio);

    /** Pedidos en alguno de los estados, con cliente y pago. El orden lo decide el BO. */
    List<PedidoVenta> listarPorEstados(Collection<EstadoVenta> estados);

    /** Pedido con cliente, entrega, pago y líneas (variante, producto, marca, color y talla). */
    Optional<PedidoVenta> obtenerPorIdCompleto(Long idPedido);

    /** Igual que obtenerPorIdCompleto, buscando por folio. */
    Optional<PedidoVenta> obtenerPorFolioCompleto(String folio);

    /** Pedidos registrados (todo estado excepto PENDIENTE), del más reciente al más antiguo. */
    List<PedidoVenta> listarRegistrados();

    /** Pedidos registrados de un cliente, del más reciente al más antiguo. */
    List<PedidoVenta> listarRegistradosPorCliente(Long clienteId);

    /**
     * Pedidos en alguno de los estados dados cuyo plazo ya venció: los registrados antes de
     * {@code limite} o, si tienen vencimiento propio (CU-04, comprobante rechazado), los que lo
     * superaron en {@code ahora}.
     */
    List<PedidoVenta> listarPorEstadosRegistradosAntesDe(Collection<EstadoVenta> estados, OffsetDateTime limite, OffsetDateTime ahora);

    /**
     * CU-04 · Página de pedidos registrados (sin carritos) que cumplen los criterios, del más
     * reciente al más antiguo, con cliente, entrega y pago. El texto de los criterios se busca
     * dentro del folio; {@code clienteIds} son los clientes cuyo nombre o teléfono coincide con ese
     * mismo texto, y sus pedidos también entran en el resultado. Sin texto se ignora.
     */
    PaginaPedidos buscarPorCriterios(CriteriosBusquedaPedidos criterios, List<Long> clienteIds);

    /** Persiste el pago de un pedido (PagoTransferencia o PagoEfectivo). */
    void guardarPago(Pago pago);

    /** Guarda o actualiza el pedido y sus líneas. */
    void guardarPedidoVenta(PedidoVenta pedidoVenta);
}
