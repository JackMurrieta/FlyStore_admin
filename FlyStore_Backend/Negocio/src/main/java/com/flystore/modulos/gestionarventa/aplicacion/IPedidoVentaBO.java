package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.contratos.administrarpedidos.BuscarPedidosPeticionDTO;
import com.flystore.contratos.administrarpedidos.DetallePedidoAdminDTO;
import com.flystore.contratos.administrarpedidos.PaginaPedidosDTO;
import com.flystore.contratos.administrarpedidos.ResultadoAccionDTO;
import com.flystore.contratos.gestionarventa.*;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.DatosEnvio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * CU-02 Gestionar venta y CU-04 Administrar pedidos. El carrito es el PedidoVenta del cliente en
 * estado PENDIENTE: se crea al agregar el primer producto, nunca se borra al vaciarse y se convierte
 * en pedido al registrarlo. Ya registrado, el personal (ADMIN o EMPLEADO) valida su pago y avanza su
 * estado; solo el cliente puede cancelarlo, y solo antes de pagar. Los carritos nunca aparecen en CU-04.
 *
 * <p>En las acciones de CU-04, {@code adminId} es el identificador de la sesión de quien ejecuta la
 * acción y queda en el historial del pedido. Todas responden CONFLICTO_DE_DATOS si el pedido ya no
 * está en el estado que necesitan, NO_ENCONTRADO si el folio no existe y DATOS_INVALIDOS si falta
 * un dato.
 */
public interface IPedidoVentaBO {

    /** Carrito del cliente con precios actualizados; vacío (sin id) si todavía no agrega productos. */
    PedidoVentaDTO obtenerPedidoEnCurso(Long clienteId);

    /** Agrega piezas de una variante; crea el carrito si no existe. Rechaza si supera el stock. */
    PedidoVentaDTO agregarAlCarrito(Long clienteId, AgregarAlCarritoPeticionDTO peticion);

    /** Alt1 · Cambia las piezas de una línea; con 0 la quita. Rechaza si supera el stock. */
    PedidoVentaDTO actualizarCantidad(Long clienteId, ActualizarCantidadPeticionDTO peticion);

    /** Alt2 · Aplica un código de descuento a los productos del carrito ligados a él. */
    PedidoVentaDTO aplicarCodigoDescuento(Long clienteId, AplicarCodigoPeticionDTO peticion);

    /** Genera (una sola vez) el folio del pedido en curso; es el concepto de la transferencia. */
    FolioDTO generarFolio(Long clienteId);

    /** Valida y guarda el comprobante de transferencia del pedido en curso; devuelve su ruta. */
    RespuestaValidacionComprobanteDTO subirComprobante(Long clienteId, String tipoPago, String folio, ArchivoComprobante archivo);

    /** Registra el pedido en curso con sus datos de entrega y forma de pago (básico, Alt3 y Alt4). */
    PedidoVentaDTO registrarPedido(Long clienteId, RegistrarPedidoPeticionDTO peticion);

    /**
     * S6 · Sube el comprobante de un pedido registrado en PENDIENTE_COMPROBANTE y lo pasa a
     * PAGO_EN_REVISION. No toca el stock; avisa si ya no alcanzan las piezas.
     */
    PedidoVentaDTO subirComprobantePendiente(Long clienteId, String folio, ArchivoComprobante archivo);

    /**
     * El cliente cancela su pedido mientras está APARTADO_PENDIENTE_PAGO o PENDIENTE_COMPROBANTE y no
     * ha vencido. Libera el código de descuento y deja la cancelación en el historial con su nombre;
     * el stock no se toca porque aún no se descontaba. Un pedido pagado o en revisión ya no se cancela.
     *
     * @param motivo opcional; puede ser null o vacío.
     */
    PedidoVentaDTO cancelarPedidoCliente(Long clienteId, String folio, String motivo);

    /** Pedido registrado del cliente, para el ticket que arma el frontend. */
    PedidoVentaDTO obtenerPedidoRegistrado(Long clienteId, String folio);

    /** ADMIN y EMPLEADO ven todos los pedidos registrados; CLIENTE solo los suyos. */
    List<PedidoVentaDTO> listarPedidos(String rolUsuario, Long clienteId);

    /** Pedido registrado por id; un CLIENTE solo puede ver los suyos. */
    PedidoVentaDTO obtenerPedidoCompleto(String idPedido, String rolUsuario, Long clienteId);

    /**
     * Pedidos que esperan confirmación de pago (PAGO_EN_REVISION y APARTADO_PENDIENTE_PAGO) en orden
     * FIFO: hora de llegada del comprobante; en efectivo, hora de registro.
     */
    List<PedidoVentaDTO> listarPorConfirmar();

    /**
     * Marca CANCELADO_VENCIMIENTO los pedidos registrados sin pago cuyo plazo de 42 h ya pasó (desde
     * el registro o desde el rechazo de su comprobante) y libera sus códigos. Devuelve cuántos.
     */
    int cancelarPedidosVencidos();

    // ===== CU-04 Administrar pedidos =====

    /** Página de pedidos registrados que cumplen los filtros, del más reciente al más antiguo. */
    PaginaPedidosDTO buscarPedidos(BuscarPedidosPeticionDTO peticion);

    /** Pedido con cliente, líneas, comprobante, envío e historial. */
    DetallePedidoAdminDTO obtenerDetallePedido(String folio);

    /**
     * Cobra en tienda un pedido APARTADO_PENDIENTE_PAGO que no ha vencido: valida el monto, calcula
     * el cambio, descuenta el stock y lo pasa a PAGADO. No avisa al cliente.
     */
    ResultadoAccionDTO registrarPagoEfectivo(String folio, BigDecimal montoRecibido, UUID adminId);

    /**
     * Aprueba el comprobante de un pedido en PAGO_EN_REVISION: descuenta el stock, deja el pago
     * COMPLETADO y pasa el pedido directo a PREPARACION. Si no alcanza el stock, el pedido conserva su estado.
     */
    ResultadoAccionDTO aprobarPago(String folio, UUID adminId);

    /**
     * Rechaza el comprobante de un pedido en PAGO_EN_REVISION: vuelve a PENDIENTE_COMPROBANTE con
     * 42 h nuevas para subir otro. No toca el stock.
     *
     * @param mensaje texto opcional para el cliente; puede ser null.
     */
    ResultadoAccionDTO rechazarComprobante(String folio, String motivo, String mensaje, UUID adminId);

    /**
     * Avanza el pedido un paso en el recorrido de su forma de pago. Efectivo: Pagado → Entregado.
     * Transferencia: En preparación → Enviado → Entregado; para ENVIADO exige paquetería y guía, o
     * entrega local. Los pedidos de la regla anterior también avanzan: una transferencia en Pagado
     * pasa a En preparación y un pedido Enviado pasa a Entregado.
     *
     * @param estado nombre del estado nuevo.
     */
    ResultadoAccionDTO actualizarEstado(String folio, String estado, DatosEnvio envio, UUID adminId);
}
