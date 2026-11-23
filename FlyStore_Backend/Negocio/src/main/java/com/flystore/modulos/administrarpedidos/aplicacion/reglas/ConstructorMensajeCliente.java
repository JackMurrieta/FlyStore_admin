package com.flystore.modulos.administrarpedidos.aplicacion.reglas;

import com.flystore.persistencia.entidades.EstadoVenta;
import com.flystore.persistencia.entidades.PedidoVenta;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Textos del aviso al cliente por cada acción del personal. Mismo tono que los del frontend. */
@Component
public class ConstructorMensajeCliente {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'a las' HH:mm", Locale.forLanguageTag("es-MX"));

    public String pagoValidado(PedidoVenta pedido) {
        return saludo(pedido) + " Confirmamos el pago de tu pedido " + pedido.getFolio() + ". ¡Gracias por tu compra!";
    }

    /** {@code mensaje} es el texto opcional que escribió el personal; puede ser null. */
    public String comprobanteRechazado(PedidoVenta pedido, String motivo, String mensaje, OffsetDateTime nuevoLimite) {
        var texto = new StringBuilder(saludo(pedido)).append(" No pudimos validar el comprobante de tu pedido ")
                .append(pedido.getFolio()).append(" (").append(motivo.trim()).append(").");
        if (mensaje != null && !mensaje.isBlank()) texto.append(' ').append(mensaje.trim());
        return texto.append(" Sube uno nuevo antes del ").append(FECHA.format(nuevoLimite))
                .append(" para conservar tu apartado.").toString();
    }

    /** Aviso de ENVIADO (con paquetería y guía, o entrega local) o de ENTREGADO. */
    public String cambioEstado(PedidoVenta pedido, String guia) {
        var inicio = saludo(pedido);
        if (pedido.getEstadoVenta() == EstadoVenta.ENTREGADO)
            return inicio + " Registramos la entrega de tu pedido " + pedido.getFolio() + ". ¡Gracias por tu compra!";
        if (guia == null || Boolean.TRUE.equals(pedido.getEntregaLocal()))
            return inicio + " Ya salió tu pedido " + pedido.getFolio() + " y te lo entregaremos de forma local.";
        return inicio + " Ya enviamos tu pedido " + pedido.getFolio() + " por " + pedido.getPaqueteria() + ", guía " + guia + ".";
    }

    public String cancelacion(PedidoVenta pedido, String motivo) {
        return saludo(pedido) + " Cancelamos tu pedido " + pedido.getFolio() + ": " + motivo.trim() + ".";
    }

    private static String saludo(PedidoVenta pedido) {
        var cliente = pedido.getCliente();
        var nombre = cliente == null || cliente.getNombre() == null ? "" : cliente.getNombre().trim();
        return (nombre.isEmpty() ? "Hola" : "Hola " + nombre) + ", te escribimos de FlyStore.";
    }
}
