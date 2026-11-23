package com.flystore.modulos.gestionarventa.aplicacion.mappers;

import com.flystore.contratos.administrarpedidos.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.PedidoAdminCompleto;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ResultadoAccion;
import com.flystore.persistencia.consultas.CriteriosBusquedaPedidos;
import com.flystore.persistencia.consultas.PaginaPedidos;
import com.flystore.persistencia.entidades.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Convierte entre los contratos de CU-04 Administrar pedidos y las entidades. No decide nada del
 * negocio: el vencimiento lo calcula PedidoVentaBO y llega como parámetro. Debe usarse dentro de la
 * transacción del BO porque recorre asociaciones LAZY (cliente, pago, líneas).
 */
@Component
public class PedidoAdminMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_TEXTO = 60;

    // ===== Petición → criterios =====

    /** Valida y convierte los filtros de la lista. Las fechas son días completos en la zona de la tienda. */
    public CriteriosBusquedaPedidos toCriterios(BuscarPedidosPeticionDTO peticion, ZoneId zona) {
        var desde = fecha(peticion.desde(), "desde");
        var hasta = fecha(peticion.hasta(), "hasta");
        if (desde != null && hasta != null && hasta.isBefore(desde))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "La fecha final no puede ser anterior a la inicial.");
        return new CriteriosBusquedaPedidos(
                texto(peticion.texto()),
                estado(peticion.estado()),
                metodoPago(peticion.metodoPago()),
                desde == null ? null : desde.atStartOfDay(zona).toOffsetDateTime(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime(),
                peticion.pagina());
    }

    // ===== Entidades → respuesta =====

    /** @param fechaLimite vencimiento del pago; null si el pedido ya no lo tiene. */
    public PedidoResumenDTO toResumenDTO(PedidoVenta pedido, OffsetDateTime fechaLimite) {
        return new PedidoResumenDTO(
                pedido.getFolio(),
                iso(pedido.getFechaHoraPedido()),
                nombreCliente(pedido),
                telefonoCliente(pedido),
                total(pedido),
                metodoPago(pedido.getPago()),
                pedido.getEstadoVenta().name(),
                iso(fechaLimite));
    }

    /** @param fechaLimite vencimiento de cada pedido (lo calcula el BO). */
    public PaginaPedidosDTO toPaginaDTO(PaginaPedidos pagina, Function<PedidoVenta, OffsetDateTime> fechaLimite) {
        return new PaginaPedidosDTO(pagina.pedidos().stream().map(p -> toResumenDTO(p, fechaLimite.apply(p))).toList(), pagina.total());
    }

    /** @param fechaLimite vencimiento del pago; null si el pedido ya no lo tiene. */
    public DetallePedidoAdminDTO toDetalleDTO(PedidoAdminCompleto completo, OffsetDateTime fechaLimite) {
        var pedido = completo.pedido();
        return new DetallePedidoAdminDTO(
                pedido.getFolio(),
                pedido.getEstadoVenta().name(),
                iso(pedido.getFechaHoraPedido()),
                iso(fechaLimite),
                new ClienteDTO(completo.cliente().getId(), nombreCliente(pedido), telefonoCliente(pedido)),
                datosEntrega(pedido.getDatosEntrega()),
                completo.lineas().stream()
                        .sorted(Comparator.comparing(DetallePedido::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toLineaDTO).toList(),
                total(pedido),
                metodoPago(pedido.getPago()),
                toComprobanteDTO(completo),
                pedido.getEntregaLocal() == null ? null
                        : new DatosEnvioDTO(pedido.getPaqueteria(), pedido.getGuia(), pedido.getEntregaLocal()),
                completo.historial().stream().map(this::toHistorialDTO).toList());
    }

    public ResultadoAccionDTO toResultadoDTO(ResultadoAccion resultado, boolean notificacionEnviada) {
        return new ResultadoAccionDTO(resultado.pedido(), notificacionEnviada);
    }

    /** Teléfono para avisar al cliente: el de su cuenta o, si no tiene, el de la entrega del pedido. */
    public String telefonoCliente(PedidoVenta pedido) {
        var cliente = pedido.getCliente();
        if (cliente != null && tieneTexto(cliente.getTelefono())) return cliente.getTelefono().trim();
        var entrega = pedido.getDatosEntrega();
        return entrega != null && tieneTexto(entrega.getTelefono()) ? entrega.getTelefono().trim() : null;
    }

    // ===== Auxiliares =====

    private LineaPedidoDTO toLineaDTO(DetallePedido detalle) {
        var variante = detalle.getVariantes().get(0).getVarianteProducto();
        var color = variante.getColor() == null ? null : variante.getColor().getNombreColor();
        var precio = valor(detalle.getPrecioUnitario()).subtract(valor(detalle.getDescuentoUnitario()));
        int cantidad = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
        return new LineaPedidoDTO(
                variante.getProducto().getNombre() + (tieneTexto(color) ? " · " + color.trim() : ""),
                variante.getTalla() == null ? null : variante.getTalla().getEtiqueta(),
                cantidad,
                dinero(precio),
                dinero(precio.multiply(BigDecimal.valueOf(cantidad))));
    }

    private ComprobanteDTO toComprobanteDTO(PedidoAdminCompleto completo) {
        var pago = completo.pagoTransferencia();
        if (pago == null || !tieneTexto(pago.getComprobantePagoUrl())) return null;
        var ruta = pago.getComprobantePagoUrl();
        // PENDIENTE con archivo: el personal lo rechazó y el cliente aún no sube otro.
        var estado = switch (pago.getEstadoPago() == null ? EstadoPago.PENDIENTE : pago.getEstadoPago()) {
            case COMPLETADO -> "COMPLETADO";
            case EN_PROCESO -> "EN_REVISION";
            case PENDIENTE -> "RECHAZADO";
        };
        // No hay lectura del archivo: el monto que dice el comprobante lo compara el personal a la vista.
        return new ComprobanteDTO(completo.urlComprobante(), ruta.substring(ruta.lastIndexOf('/') + 1), null,
                iso(pago.getFechaHoraComprobante()), pago.getNumeroReferencia(), estado);
    }

    private HistorialEntradaDTO toHistorialDTO(HistorialPedido entrada) {
        return new HistorialEntradaDTO(iso(entrada.getFechaHora()), entrada.getUsuario(),
                entrada.getEstadoAnterior() == null ? null : entrada.getEstadoAnterior().name(),
                entrada.getEstadoNuevo().name(), entrada.getMotivo());
    }

    private static String nombreCliente(PedidoVenta pedido) {
        var cliente = pedido.getCliente();
        var nombre = cliente == null ? "" : String.join(" ", Stream.of(cliente.getNombre(), cliente.getApellidos())
                .filter(PedidoAdminMapper::tieneTexto).map(String::trim).toList());
        if (!nombre.isEmpty()) return nombre;
        var entrega = pedido.getDatosEntrega();
        return entrega != null && tieneTexto(entrega.getNombreCompleto()) ? entrega.getNombreCompleto().trim() : "Cliente";
    }

    private static String datosEntrega(DatosEntrega entrega) {
        if (entrega == null) return "Sin datos de entrega";
        var lugar = String.join(", ", Stream.of(entrega.getDireccion(), entrega.getCiudad(), entrega.getEstado())
                .filter(PedidoAdminMapper::tieneTexto).map(String::trim).toList());
        var texto = new StringBuilder();
        if (tieneTexto(entrega.getNombreCompleto())) texto.append("Recibe ").append(entrega.getNombreCompleto().trim());
        if (!lugar.isEmpty()) texto.append(texto.isEmpty() ? "" : " · ").append(lugar);
        if (tieneTexto(entrega.getReferencias())) texto.append(texto.isEmpty() ? "" : " · ").append("Ref.: ").append(entrega.getReferencias().trim());
        return texto.isEmpty() ? "Sin datos de entrega" : texto.toString();
    }

    /** Total cobrado: el monto del pago; en un pedido sin pago, la suma de sus líneas más el envío. */
    private static BigDecimal total(PedidoVenta pedido) {
        if (pedido.getPago() != null && pedido.getPago().getMonto() != null) return dinero(pedido.getPago().getMonto());
        var suma = valor(pedido.getCostoEnvio());
        if (pedido.getDetalles() != null) {
            for (var d : pedido.getDetalles()) {
                var precio = valor(d.getPrecioUnitario()).subtract(valor(d.getDescuentoUnitario()));
                suma = suma.add(precio.multiply(BigDecimal.valueOf(d.getCantidad() == null ? 0 : d.getCantidad())));
            }
        }
        return dinero(suma);
    }

    private static String metodoPago(Pago pago) {
        return pago != null && pago.sinProxy() instanceof PagoTransferencia
                ? CriteriosBusquedaPedidos.TRANSFERENCIA : CriteriosBusquedaPedidos.EFECTIVO;
    }

    private static String texto(String valor) {
        if (valor == null) return null;
        // Los comodines de LIKE no se buscan literalmente.
        var limpio = valor.replaceAll("[%_]", "").trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() > MAX_TEXTO ? limpio.substring(0, MAX_TEXTO) : limpio;
    }

    private static EstadoVenta estado(String valor) {
        if (!tieneTexto(valor)) return null;
        try {
            var estado = EstadoVenta.valueOf(valor.trim().toUpperCase(Locale.ROOT));
            // PENDIENTE es el carrito del cliente: no es un pedido que administre el personal.
            if (estado != EstadoVenta.PENDIENTE) return estado;
        } catch (IllegalArgumentException e) {
            // cae al error de abajo
        }
        throw new NegocioException(TipoError.DATOS_INVALIDOS, "Estado de pedido inválido.");
    }

    private static String metodoPago(String valor) {
        if (!tieneTexto(valor)) return null;
        var metodo = valor.trim().toUpperCase(Locale.ROOT);
        if (metodo.equals(CriteriosBusquedaPedidos.TRANSFERENCIA) || metodo.equals(CriteriosBusquedaPedidos.EFECTIVO)) return metodo;
        throw new NegocioException(TipoError.DATOS_INVALIDOS, "Método de pago inválido.");
    }

    private static LocalDate fecha(String valor, String campo) {
        if (!tieneTexto(valor)) return null;
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "La fecha «" + campo + "» debe tener el formato AAAA-MM-DD.");
        }
    }

    private static String iso(OffsetDateTime fecha) {
        return fecha == null ? null : ISO.format(fecha);
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static BigDecimal valor(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto;
    }

    private static BigDecimal dinero(BigDecimal monto) {
        return valor(monto).setScale(2, RoundingMode.HALF_UP);
    }
}
