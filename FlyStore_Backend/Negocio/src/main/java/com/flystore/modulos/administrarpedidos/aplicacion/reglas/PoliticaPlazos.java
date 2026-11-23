package com.flystore.modulos.administrarpedidos.aplicacion.reglas;

import com.flystore.modulos.gestionarventa.aplicacion.PedidoVentaBO;
import com.flystore.persistencia.entidades.PedidoVenta;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

/**
 * Plazo de 42 h para pagar o subir el comprobante. Es la misma regla de CU-02 (sus constantes):
 * corre desde el registro del pedido, salvo que el personal haya rechazado un comprobante, en cuyo
 * caso manda el vencimiento nuevo guardado en el pedido.
 */
@Component
public class PoliticaPlazos {

    /** Vencimiento de un pedido que sigue sin pago; null en los demás estados. */
    public OffsetDateTime fechaLimite(PedidoVenta pedido) {
        if (!PedidoVentaBO.ESTADOS_SIN_PAGO.contains(pedido.getEstadoVenta())) return null;
        if (pedido.getFechaLimitePago() != null) return pedido.getFechaLimitePago();
        return pedido.getFechaHoraPedido() == null ? null : pedido.getFechaHoraPedido().plus(PedidoVentaBO.PLAZO_PAGO);
    }

    /** El pedido sigue sin pago y su plazo ya pasó. */
    public boolean estaVencido(PedidoVenta pedido, OffsetDateTime ahora) {
        var limite = fechaLimite(pedido);
        return limite != null && ahora.isAfter(limite);
    }

    /** Vencimiento que recibe el pedido cuando se rechaza su comprobante: 42 h desde ahora. */
    public OffsetDateTime nuevoLimiteComprobante(OffsetDateTime ahora) {
        return ahora.plus(PedidoVentaBO.PLAZO_PAGO);
    }
}
