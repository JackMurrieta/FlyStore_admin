package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.contratos.gestionarventa.*;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import java.util.List;

/**
 * CU-02 Gestionar venta. El carrito es el PedidoVenta del cliente en estado PENDIENTE: se crea al
 * agregar el primer producto, nunca se borra al vaciarse y se convierte en pedido al registrarlo.
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
     * El personal confirma el pago: descuenta el stock y el pedido pasa a PAGADO con el pago COMPLETADO.
     * En efectivo exige la cantidad recibida y calcula el cambio. Si no alcanza el stock lanza
     * CONFLICTO_DE_DATOS y el pedido conserva su estado.
     */
    PedidoVentaDTO confirmarPago(String idPedido, ConfirmarPagoPeticionDTO peticion);

    /** Enlace temporal al comprobante de transferencia del pedido, para revisarlo. */
    ComprobanteUrlDTO obtenerUrlComprobante(String idPedido);

    /** Marca CANCELADO_VENCIMIENTO los pedidos registrados sin pago hace más de 42 h. Devuelve cuántos. */
    int cancelarPedidosVencidos();
}
