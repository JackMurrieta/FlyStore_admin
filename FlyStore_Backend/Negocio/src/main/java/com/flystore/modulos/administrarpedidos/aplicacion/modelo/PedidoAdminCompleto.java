package com.flystore.modulos.administrarpedidos.aplicacion.modelo;

import com.flystore.persistencia.entidades.*;
import java.util.List;

/**
 * Todo lo que muestra el detalle de un pedido: el pedido con su cliente, líneas y pago, su
 * historial y el enlace temporal al comprobante (vacío si no hay o no se pudo generar).
 */
public record PedidoAdminCompleto(PedidoVenta pedido, List<HistorialPedido> historial, String urlComprobante) {

    public PedidoAdminCompleto {
        historial = historial == null ? List.of() : List.copyOf(historial);
        urlComprobante = urlComprobante == null ? "" : urlComprobante;
    }

    public Cliente cliente() {
        return pedido.getCliente();
    }

    public List<DetallePedido> lineas() {
        return pedido.getDetalles() == null ? List.of() : pedido.getDetalles();
    }

    /** Pago por transferencia del pedido; null si pagó en efectivo. */
    public PagoTransferencia pagoTransferencia() {
        return pedido.getPago() != null && pedido.getPago().sinProxy() instanceof PagoTransferencia t ? t : null;
    }

    /** Pago en efectivo del pedido; null si pagó por transferencia. */
    public PagoEfectivo pagoEfectivo() {
        return pedido.getPago() != null && pedido.getPago().sinProxy() instanceof PagoEfectivo e ? e : null;
    }
}
