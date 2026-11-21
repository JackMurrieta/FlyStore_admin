package com.flystore.modulos.gestionarventa.aplicacion.mappers;

import com.flystore.contratos.gestionarventa.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.TotalesPedido;
import com.flystore.persistencia.entidades.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Conversión entre entidades de la venta y los DTOs de CU-02. No decide reglas: los totales y los
 * precios llegan calculados por los BO.
 */
@Component
public class PedidoVentaMapper {

    public static final String SIN_TALLA = "Única";

    public PedidoVentaDTO aPedidoVentaDTO(PedidoVenta pedido, TotalesPedido totales,
                                         OffsetDateTime fechaLimitePago, String enlaceWhatsapp) {
        var detalles = pedido.getDetalles() == null ? List.<DetallePedidoDTO>of() : pedido.getDetalles().stream()
                .sorted(Comparator.comparing(DetallePedido::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(d -> aDetallePedidoDTO(d, totales.preciosBase().get(producto(d).getId())))
                .toList();
        return new PedidoVentaDTO(
                pedido.getId(),
                pedido.getFolio(),
                pedido.getEstadoVenta() == null ? null : pedido.getEstadoVenta().name(),
                detalles,
                totales.totalPiezas(),
                totales.tipoPrecioAplicado(),
                totales.siguienteTipoPrecio(),
                totales.piezasParaSiguienteNivel(),
                totales.nivelesPrecio().stream().map(this::aNivelPrecioDTO).toList(),
                dinero(totales.subtotalPrecioBase()),
                dinero(totales.ahorroPorTipoPrecio()),
                dinero(totales.totalDescuentos()),
                dinero(totales.costoEnvio()),
                dinero(totales.total()),
                pedido.getDatosEntrega() == null ? null : aDatosEntregaDTO(pedido.getDatosEntrega(), false),
                pedido.getPago() == null ? null : aMetodoPagoDTO(pedido.getPago(), pedido.getFolio()),
                pedido.getEstadoVenta() == EstadoVenta.PENDIENTE ? null : pedido.getFechaHoraPedido(),
                fechaLimitePago,
                enlaceWhatsapp);
    }

    /** Carrito de un cliente que todavía no agrega productos: no existe en la base. */
    public PedidoVentaDTO aPedidoVentaVacioDTO() {
        return new PedidoVentaDTO(null, null, EstadoVenta.PENDIENTE.name(), List.of(), 0, TipoPrecio.BASE.name(),
                null, null, List.of(), dinero(BigDecimal.ZERO), dinero(BigDecimal.ZERO), dinero(BigDecimal.ZERO),
                dinero(BigDecimal.ZERO), dinero(BigDecimal.ZERO), null, null, null, null, null);
    }

    public DetallePedidoDTO aDetallePedidoDTO(DetallePedido detalle, BigDecimal precioBase) {
        var variante = detalle.getVariantes().get(0).getVarianteProducto();
        var producto = variante.getProducto();
        var precio = valor(detalle.getPrecioUnitario());
        var descuento = valor(detalle.getDescuentoUnitario());
        var cantidad = BigDecimal.valueOf(detalle.getCantidad());
        var codigo = detalle.getCodigosAplicados() == null ? null : detalle.getCodigosAplicados().stream()
                .map(DetallePedidoCodigoDescuento::getCodigoDescuento).filter(Objects::nonNull).findFirst()
                .map(c -> new CodigoDescuentoAplicadoDTO(c.getCodigo(), "$" + dinero(c.getMontoFijo()).toPlainString() + " por pieza",
                        dinero(descuento.multiply(cantidad))))
                .orElse(null);
        return new DetallePedidoDTO(
                detalle.getId(),
                variante.getId(),
                producto.getNombre(),
                producto.getMarca() == null ? null : producto.getMarca().getNombre(),
                variante.getTalla() == null || variante.getTalla().getTipoTalla() == null ? "Talla" : variante.getTalla().getTipoTalla().getNombre(),
                variante.getTalla() == null ? SIN_TALLA : variante.getTalla().getEtiqueta(),
                variante.getColor() == null ? null : variante.getColor().getNombreColor(),
                imagen(variante),
                detalle.getCantidad(),
                variante.getStock(),
                detalle.getTipoPrecio() == null ? null : detalle.getTipoPrecio().name(),
                dinero(precioBase == null ? precio : precioBase),
                dinero(precio),
                dinero(descuento),
                codigo,
                dinero(precio.subtract(descuento).multiply(cantidad)));
    }

    public DatosEntregaDTO aDatosEntregaDTO(DatosEntrega datos, boolean predeterminada) {
        return new DatosEntregaDTO(datos.getNombreCompleto(), datos.getTelefono(), datos.getCiudad(),
                datos.getEstado(), datos.getDireccion(), datos.getReferencias(), predeterminada);
    }

    /** Datos de un cliente que nunca ha capturado su entrega: se proponen su nombre y teléfono. */
    public DatosEntregaDTO aDatosEntregaDTO(Cliente cliente) {
        var nombre = String.join(" ", java.util.stream.Stream.of(cliente.getNombre(), cliente.getApellidos())
                .filter(s -> s != null && !s.isBlank()).map(String::trim).toList());
        return new DatosEntregaDTO(nombre.isBlank() ? null : nombre, cliente.getTelefono(), null, null, null, null, false);
    }

    public DatosEntrega aDatosEntrega(DatosEntregaDTO dto) {
        var datos = new DatosEntrega();
        datos.setNombreCompleto(dto.nombreCompleto().trim());
        datos.setTelefono(dto.telefono().trim());
        datos.setCiudad(dto.ciudad().trim());
        datos.setEstado(dto.estado().trim());
        datos.setDireccion(dto.direccion().trim());
        datos.setReferencias(dto.referencias() == null || dto.referencias().isBlank() ? null : dto.referencias().trim());
        datos.setActivo(true);
        return datos;
    }

    public MetodoPagoDTO aMetodoPagoDTO(Pago pago, String folio) {
        var real = pago.sinProxy();
        if (real instanceof PagoTransferencia transferencia)
            return new MetodoPagoDTO("TRANSFERENCIA", folio, transferencia.getComprobantePagoUrl(), null,
                    transferencia.getFechaHoraComprobante(), null, null);
        if (real instanceof PagoEfectivo efectivo)
            return new MetodoPagoDTO("EFECTIVO", folio, null, null, null,
                    efectivo.getCantidadRecibida() == null ? null : dinero(efectivo.getCantidadRecibida()),
                    efectivo.getCambio() == null ? null : dinero(efectivo.getCambio()));
        return new MetodoPagoDTO("EFECTIVO", folio, null, null);
    }

    public DatosBancariosDTO aDatosBancariosDTO(DatoBancario dato) {
        return new DatosBancariosDTO(dato.getNombreBanco(), dato.getTitular(), dato.getNumeroCuenta(), dato.getClabe());
    }

    public NivelPrecioDTO aNivelPrecioDTO(PrecioProducto precio) {
        return new NivelPrecioDTO(precio.getTipoPrecio().name(), precio.getMinPiezas());
    }

    /** Identificador numérico recibido como texto; responde 400 si no es válido. */
    public static Long id(String valor) {
        try {
            long id = Long.parseLong(valor.trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException | NullPointerException e) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Identificador inválido");
        }
    }

    private static Producto producto(DetallePedido detalle) {
        return detalle.getVariantes().get(0).getVarianteProducto().getProducto();
    }

    /** Primera imagen del color de la variante; si no hay, la primera del producto. */
    private static String imagen(VarianteProducto variante) {
        var imagenes = variante.getProducto().getImagenes();
        if (imagenes == null || imagenes.isEmpty()) return null;
        var porOrden = Comparator.comparing(ImagenProducto::getOrden, Comparator.nullsLast(Comparator.naturalOrder()));
        var colorId = variante.getColor() == null ? null : variante.getColor().getId();
        return imagenes.stream()
                .filter(i -> colorId != null && i.getColor() != null && colorId.equals(i.getColor().getId()))
                .min(porOrden)
                .or(() -> imagenes.stream().min(porOrden))
                .map(ImagenProducto::getUrlImagen)
                .orElse(null);
    }

    private static BigDecimal valor(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto;
    }

    private static BigDecimal dinero(BigDecimal monto) {
        return valor(monto).setScale(2, RoundingMode.HALF_UP);
    }
}
