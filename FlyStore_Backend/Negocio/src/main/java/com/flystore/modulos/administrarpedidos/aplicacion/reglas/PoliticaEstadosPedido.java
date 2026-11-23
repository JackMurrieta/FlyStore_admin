package com.flystore.modulos.administrarpedidos.aplicacion.reglas;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.administrarpedidos.aplicacion.modelo.DatosEnvio;
import com.flystore.persistencia.entidades.EstadoVenta;
import com.flystore.persistencia.entidades.PedidoVenta;
import org.springframework.stereotype.Component;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Qué puede hacer el personal con un pedido según su estado. Un pedido que cambió de estado
 * mientras alguien lo tenía abierto responde CONFLICTO_DE_DATOS (409) para que lo vuelva a cargar.
 */
@Component
public class PoliticaEstadosPedido {

    /** Recorrido que el personal avanza a mano una vez pagado el pedido. */
    private static final Map<EstadoVenta, EstadoVenta> SIGUIENTE = Map.of(
            EstadoVenta.PAGADO, EstadoVenta.ENVIADO,
            EstadoVenta.PREPARACION, EstadoVenta.ENVIADO,
            EstadoVenta.ENVIADO, EstadoVenta.ENTREGADO);

    private static final Set<EstadoVenta> CANCELABLES = EnumSet.of(EstadoVenta.APARTADO_PENDIENTE_PAGO,
            EstadoVenta.PENDIENTE_COMPROBANTE, EstadoVenta.PAGO_EN_REVISION, EstadoVenta.PAGADO, EstadoVenta.PREPARACION);

    private static final Map<EstadoVenta, String> ETIQUETAS = Map.of(
            EstadoVenta.PENDIENTE, "En carrito",
            EstadoVenta.APARTADO_PENDIENTE_PAGO, "Apartado · pendiente de pago",
            EstadoVenta.PENDIENTE_COMPROBANTE, "Pendiente de comprobante",
            EstadoVenta.PAGO_EN_REVISION, "Pago en revisión",
            EstadoVenta.PAGADO, "Pagado",
            EstadoVenta.PREPARACION, "En preparación",
            EstadoVenta.ENVIADO, "Enviado",
            EstadoVenta.ENTREGADO, "Entregado",
            EstadoVenta.CANCELADO, "Cancelado",
            EstadoVenta.CANCELADO_VENCIMIENTO, "Cancelado por vencimiento");

    /** Nombre del estado como lo ve el personal. */
    public String etiqueta(EstadoVenta estado) {
        return ETIQUETAS.getOrDefault(estado, String.valueOf(estado));
    }

    /** Lanza CONFLICTO_DE_DATOS si el pedido ya no está en el estado que la acción necesita. */
    public void exigirEstado(PedidoVenta pedido, EstadoVenta esperado) {
        if (pedido.getEstadoVenta() != esperado)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS, "El pedido " + pedido.getFolio() + " ya no está en «"
                    + etiqueta(esperado) + "»: ahora está en «" + etiqueta(pedido.getEstadoVenta()) + "». Vuelve a cargarlo.");
    }

    /** Estado al que se puede avanzar desde el actual; null si ya no admite cambios. */
    public EstadoVenta siguientePermitido(EstadoVenta actual) {
        return SIGUIENTE.get(actual);
    }

    /** Lanza CONFLICTO_DE_DATOS si el cambio se salta la secuencia Pagado → Enviado → Entregado. */
    public void validarTransicion(EstadoVenta actual, EstadoVenta nuevo) {
        var siguiente = siguientePermitido(actual);
        if (siguiente == null)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                    "Un pedido en «" + etiqueta(actual) + "» no admite cambio de estado.");
        if (siguiente != nuevo)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                    "Desde «" + etiqueta(actual) + "» solo se puede pasar a «" + etiqueta(siguiente) + "».");
    }

    /** Para marcar ENVIADO hacen falta paquetería y guía, o entrega local. Lanza DATOS_INVALIDOS si faltan. */
    public void requiereDatosEnvio(EstadoVenta nuevo, DatosEnvio envio) {
        if (nuevo == EstadoVenta.ENVIADO && (envio == null || !envio.completo()))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Indica paquetería y número de guía, o marca entrega local.");
    }

    /** Un pedido se puede cancelar mientras no haya salido de la tienda ni esté ya cancelado. */
    public boolean puedeCancelarse(EstadoVenta estado) {
        return CANCELABLES.contains(estado);
    }
}
