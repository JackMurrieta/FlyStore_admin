package com.flystore.modulos.gestionarventa;

import com.flystore.contratos.gestionarventa.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.IClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.*;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.IAlmacenComprobantes;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

class PedidoVentaBOTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), ZoneId.of("America/Hermosillo"));
    private static final byte[] PDF = "%PDF-1.4 comprobante".getBytes(StandardCharsets.US_ASCII);

    private final Cliente cliente = DatosVenta.cliente(7);
    private final Producto gorra = DatosVenta.gorra(1);
    private final VarianteProducto roja = DatosVenta.variante(10, gorra, "Rojo", 8);
    private final CodigoDescuento fly10 = DatosVenta.codigo(3, "FLY10", "50.00");
    private PedidoVenta carrito;
    private final List<PedidoVenta> pedidos = new ArrayList<>();
    private long siguienteIdPedido = 20;
    private List<PedidoVenta> vencidos = List.of();
    private final List<Pago> pagos = new ArrayList<>();
    private final List<String> subidos = new ArrayList<>();
    private long siguienteIdLinea = 100;

    private final PedidoVentaBO bo = crearBO();

    private PedidoVentaBO crearBO() {
        var pedidoDAO = puerto(IPedidoVentaDAO.class, (m, a) -> switch (m) {
            case "bloquearPedidoPendienteCliente", "obtenerPedidoPendienteCliente" -> Optional.ofNullable(carrito);
            case "crearPedidoVenta" -> { carrito = DatosVenta.carrito(siguienteIdPedido++, cliente); pedidos.add(carrito); yield carrito; }
            case "bloquearPorId", "obtenerPorIdCompleto" -> pedidos.stream().filter(p -> p.getId().equals(a[0])).findFirst();
            case "bloquearPorFolio" -> pedidos.stream().filter(p -> a[0].equals(p.getFolio())).findFirst();
            case "listarPorEstados" -> pedidos.stream().filter(p -> ((Collection<?>) a[0]).contains(p.getEstadoVenta())).toList();
            case "guardarPedidoVenta" -> { simularFlush((PedidoVenta) a[0]); yield null; }
            case "guardarPago" -> { pagos.add((Pago) a[0]); yield null; }
            case "listarPorEstadosRegistradosAntesDe" -> vencidos;
            default -> throw new AssertionError("IPedidoVentaDAO." + m);
        });
        var clienteDAO = puerto(IClienteDAO.class, (m, a) -> {
            if (m.equals("bloquearPorId")) return Optional.of(cliente);
            throw new AssertionError("IClienteDAO." + m);
        });
        var varianteDAO = puerto(IVarianteProductoDAO.class, (m, a) -> roja.getId().equals(a[0]) ? Optional.of(roja) : Optional.empty());
        var codigoDAO = puerto(ICodigoDescuentoDAO.class, (m, a) -> switch (m) {
            case "obtenerPorCodigo" -> "FLY10".equalsIgnoreCase(((String) a[0]).trim()) ? Optional.of(fly10) : Optional.empty();
            case "bloquearPorId" -> Optional.of(fly10);
            case "obtenerIdsProductosAplicables" -> List.of(gorra.getId());
            default -> throw new AssertionError("ICodigoDescuentoDAO." + m);
        });
        var clienteBO = puerto(IClienteBO.class, (m, a) -> {
            if (!m.equals("registrarDatosEntrega")) throw new AssertionError("IClienteBO." + m);
            var dto = (DatosEntregaDTO) a[1];
            var datos = new DatosEntrega();
            datos.setNombreCompleto(dto.nombreCompleto());
            datos.setTelefono(dto.telefono());
            datos.setCiudad(dto.ciudad());
            datos.setEstado(dto.estado());
            datos.setDireccion(dto.direccion());
            return datos;
        });
        IAlmacenComprobantes almacen = puerto(IAlmacenComprobantes.class, (m, a) -> switch (m) {
            case "subir" -> { subidos.add((String) a[0]); yield a[0]; }
            case "generarUrlTemporal" -> "https://firmado/" + a[0] + "?expira=" + a[1];
            default -> throw new AssertionError("IAlmacenComprobantes." + m);
        });
        var productoVentaBO = new ProductoVentaBO();
        return new PedidoVentaBO(pedidoDAO, clienteDAO, new DetallePedidoBO(productoVentaBO), productoVentaBO,
                new VarianteProductoBO(varianteDAO), new CodigoDescuentoBO(codigoDAO, RELOJ), clienteBO, almacen,
                new PedidoVentaMapper(), RELOJ, "52 662 123 4567");
    }

    /** Como el flush de JPA: asigna id a las líneas nuevas. */
    private void simularFlush(PedidoVenta pedido) {
        for (var linea : pedido.getDetalles()) if (linea.getId() == null) DatosVenta.conId(linea, siguienteIdLinea++);
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " y fue " + real);
    }

    private static DatosEntregaDTO entrega() {
        return new DatosEntregaDTO("Ana López", "6621234567", "Hermosillo", "Sonora", "Calle Uno 123", null, true);
    }

    @Test void sinCarritoDevuelveUnoVacioSinCrearlo() {
        var dto = bo.obtenerPedidoEnCurso(7L);
        assertNull(dto.id());
        assertTrue(dto.detalles().isEmpty());
        assertNull(carrito, "Ver el carrito no crea un PedidoVenta");
    }

    @Test void agregarCreaElCarritoYCalculaElEsquemaPorPiezas() {
        var dto = bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 6));
        assertNotNull(carrito);
        assertEquals(20L, dto.id());
        assertEquals("PENDIENTE", dto.estadoVenta());
        assertEquals(6, dto.totalPiezas());
        assertEquals("MAYOREO", dto.tipoPrecioAplicado());
        assertEquals("DISTRIBUIDOR", dto.siguienteTipoPrecio());
        assertEquals(6, dto.piezasParaSiguienteNivel());
        assertEquals(3, dto.nivelesPrecio().size());
        assertMonto("900.00", dto.subtotalPrecioBase());
        assertMonto("120.00", dto.ahorroPorTipoPrecio());
        assertMonto("780.00", dto.total());
        var linea = dto.detalles().get(0);
        assertEquals(10L, linea.varianteProductoId());
        assertEquals("New Era", linea.marca());
        assertEquals(PedidoVentaMapper.SIN_TALLA, linea.talla());
        assertEquals(8, linea.existencia());
        assertMonto("150.00", linea.precioBase());
        assertMonto("130.00", linea.precioUnitario());
        assertMonto("780.00", linea.subtotal());
    }

    @Test void agregarRechazaPiezasQueSuperanElStock() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 6));
        var error = assertThrows(NegocioException.class, () -> bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 3)));
        assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
        assertTrue(error.getMessage().startsWith("Solo hay 8 piezas"), error.getMessage());
        assertThrows(NegocioException.class, () -> bo.actualizarCantidad(7L, new ActualizarCantidadPeticionDTO(10L, 9)));
    }

    @Test void cambiarCantidadRecalculaElEsquemaYCeroVaciaElCarrito() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 6));
        assertEquals("BASE", bo.actualizarCantidad(7L, new ActualizarCantidadPeticionDTO(10L, 2)).tipoPrecioAplicado());
        var vacio = bo.actualizarCantidad(7L, new ActualizarCantidadPeticionDTO(10L, 0));
        assertTrue(vacio.detalles().isEmpty());
        assertEquals(20L, vacio.id(), "El carrito vacío no se borra");
    }

    @Test void codigoDeDescuentoRestaPorPieza() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 2));
        var dto = bo.aplicarCodigoDescuento(7L, new AplicarCodigoPeticionDTO(" fly10 "));
        assertMonto("100.00", dto.totalDescuentos());
        assertMonto("200.00", dto.total());
        assertEquals("FLY10", dto.detalles().get(0).codigoDescuento().codigo());
        assertMonto("100.00", dto.detalles().get(0).codigoDescuento().montoDescuento());
        var error = assertThrows(NegocioException.class, () -> bo.aplicarCodigoDescuento(7L, new AplicarCodigoPeticionDTO("NOEXISTE")));
        assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
    }

    @Test void codigoVencidoSeQuitaAlVerElCarrito() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 2));
        bo.aplicarCodigoDescuento(7L, new AplicarCodigoPeticionDTO("FLY10"));
        fly10.setFechaFin(LocalDate.of(2026, 11, 19));
        var dto = bo.obtenerPedidoEnCurso(7L);
        assertNull(dto.detalles().get(0).codigoDescuento());
        assertMonto("300.00", dto.total());
    }

    @Test void transferenciaConComprobanteQuedaEnRevisionSinDescontarStock() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 2));
        bo.aplicarCodigoDescuento(7L, new AplicarCodigoPeticionDTO("FLY10"));
        var folio = bo.generarFolio(7L).folio();
        assertEquals("FLY-000020", folio);
        assertEquals(folio, bo.generarFolio(7L).folio(), "El folio se genera una sola vez");

        var respuesta = bo.subirComprobante(7L, "TRANSFERENCIA", folio, new ArchivoComprobante(PDF, "application/octet-stream", "pago.pdf"));
        assertTrue(respuesta.valido());
        assertTrue(respuesta.comprobanteRuta().matches("pedidos/7/FLY-000020/[0-9a-f-]{36}\\.pdf"), respuesta.comprobanteRuta());
        assertEquals(List.of(respuesta.comprobanteRuta()), subidos);

        var registrado = bo.registrarPedido(7L, new RegistrarPedidoPeticionDTO(entrega(),
                new MetodoPagoDTO("TRANSFERENCIA", folio, respuesta.comprobanteRuta(), "pago.pdf"), new BigDecimal("200")));
        assertEquals("PAGO_EN_REVISION", registrado.estadoVenta());
        assertEquals(8, roja.getStock(), "El stock solo se descuenta al confirmar el pago");
        assertEquals(OffsetDateTime.now(RELOJ), registrado.metodoPago().fechaHoraComprobante());
        assertEquals(1, fly10.getUsosActuales());
        assertNull(registrado.fechaLimitePago());
        assertEquals("Hermosillo", registrado.datosEntrega().ciudad());
        assertEquals(respuesta.comprobanteRuta(), registrado.metodoPago().comprobanteRuta());
        assertEquals("https://wa.me/526621234567?text=Hola%2C%20te%20escribo%20por%20mi%20pedido%20FLY-000020", registrado.enlaceWhatsapp());
        var pago = (PagoTransferencia) pagos.get(0);
        assertEquals(EstadoPago.EN_PROCESO, pago.getEstadoPago());
        assertNull(pago.getFechaPago(), "La fecha de pago se asigna al confirmarlo");
        assertEquals("FLY-000020", pago.getNumeroReferencia());
        assertMonto("200.00", pago.getMonto());
    }

    @Test void efectivoYTransferenciaSinComprobanteNoApartanPiezas() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 2));
        var folio = bo.generarFolio(7L).folio();
        var registrado = bo.registrarPedido(7L, new RegistrarPedidoPeticionDTO(entrega(), new MetodoPagoDTO("EFECTIVO", folio, null, null), null));
        assertEquals("APARTADO_PENDIENTE_PAGO", registrado.estadoVenta());
        assertEquals(8, roja.getStock());
        assertEquals(OffsetDateTime.now(RELOJ).plusHours(42), registrado.fechaLimitePago());
        assertInstanceOf(PagoEfectivo.class, pagos.get(0));

        carrito = null;
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 1));
        var otroFolio = bo.generarFolio(7L).folio();
        var sinComprobante = bo.registrarPedido(7L, new RegistrarPedidoPeticionDTO(entrega(), new MetodoPagoDTO("TRANSFERENCIA", otroFolio, null, null), null));
        assertEquals("PENDIENTE_COMPROBANTE", sinComprobante.estadoVenta());
        assertEquals(8, roja.getStock());
    }

    @Test void registrarRechazaTotalDistintoFolioAjenoYComprobanteDeOtroCliente() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 2));
        var folio = bo.generarFolio(7L).folio();
        var conflicto = assertThrows(NegocioException.class, () -> bo.registrarPedido(7L,
                new RegistrarPedidoPeticionDTO(entrega(), new MetodoPagoDTO("EFECTIVO", folio, null, null), new BigDecimal("1"))));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, conflicto.getTipoError());
        assertThrows(NegocioException.class, () -> bo.registrarPedido(7L,
                new RegistrarPedidoPeticionDTO(entrega(), new MetodoPagoDTO("EFECTIVO", "FLY-999999", null, null), null)));
        var ajena = "pedidos/8/" + folio + "/" + UUID.randomUUID() + ".pdf";
        var error = assertThrows(NegocioException.class, () -> bo.registrarPedido(7L,
                new RegistrarPedidoPeticionDTO(entrega(), new MetodoPagoDTO("TRANSFERENCIA", folio, ajena, null), null)));
        assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
        assertTrue(pagos.isEmpty());
        assertEquals(EstadoVenta.PENDIENTE, carrito.getEstadoVenta());
    }

    @Test void comprobanteRechazadoAlt5() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 1));
        var folio = bo.generarFolio(7L).folio();
        var texto = new ArchivoComprobante("hola".getBytes(StandardCharsets.UTF_8), "application/pdf", "pago.pdf");
        var error = assertThrows(NegocioException.class, () -> bo.subirComprobante(7L, "TRANSFERENCIA", folio, texto));
        assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
        var grande = new ArchivoComprobante(new byte[ArchivoComprobante.MAX_BYTES + 1], "image/png", "pago.png");
        assertThrows(NegocioException.class, () -> bo.subirComprobante(7L, "TRANSFERENCIA", folio, grande));
        var pdf = new ArchivoComprobante(PDF, "application/pdf", "pago.pdf");
        assertThrows(NegocioException.class, () -> bo.subirComprobante(7L, "TRANSFERENCIA", "FLY-000001", pdf));
        assertThrows(NegocioException.class, () -> bo.subirComprobante(7L, "EFECTIVO", folio, pdf));
        assertTrue(subidos.isEmpty());
    }

    /** Registra un pedido de la gorra roja con la forma de pago indicada y devuelve su DTO. */
    private PedidoVentaDTO registrar(int piezas, String tipoPago, boolean conComprobante) {
        carrito = null;
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, piezas));
        var folio = bo.generarFolio(7L).folio();
        String ruta = null;
        if (conComprobante)
            ruta = bo.subirComprobante(7L, "TRANSFERENCIA", folio, new ArchivoComprobante(PDF, "application/pdf", "pago.pdf")).comprobanteRuta();
        return bo.registrarPedido(7L, new RegistrarPedidoPeticionDTO(entrega(), new MetodoPagoDTO(tipoPago, folio, ruta, null), null));
    }

    @Test void subirComprobanteAvisaSiYaNoAlcanzaElStock() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 5));
        var folio = bo.generarFolio(7L).folio();
        roja.setStock(3);
        var error = assertThrows(NegocioException.class, () -> bo.subirComprobante(7L, "TRANSFERENCIA", folio,
                new ArchivoComprobante(PDF, "application/pdf", "pago.pdf")));
        assertTrue(error.getMessage().startsWith("Solo hay 3 piezas"), error.getMessage());
        assertTrue(subidos.isEmpty(), "No se guarda el comprobante si el cliente debe ajustar su carrito");
    }

    @Test void comprobanteDespuesPasaARevisionSinTocarStock() {
        var registrado = registrar(2, "TRANSFERENCIA", false);
        assertEquals("PENDIENTE_COMPROBANTE", registrado.estadoVenta());
        var pdf = new ArchivoComprobante(PDF, "application/pdf", "pago.pdf");
        var enRevision = bo.subirComprobantePendiente(7L, registrado.folio(), pdf);
        assertEquals("PAGO_EN_REVISION", enRevision.estadoVenta());
        assertEquals(8, roja.getStock());
        assertNull(enRevision.fechaLimitePago(), "En revisión ya no corre el plazo de 42 h");
        assertTrue(enRevision.metodoPago().comprobanteRuta().startsWith("pedidos/7/" + registrado.folio() + "/"));
        var otraVez = assertThrows(NegocioException.class, () -> bo.subirComprobantePendiente(7L, registrado.folio(), pdf));
        assertEquals(TipoError.DATOS_INVALIDOS, otraVez.getTipoError());
        var ajeno = assertThrows(NegocioException.class, () -> bo.subirComprobantePendiente(8L, registrado.folio(), pdf));
        assertEquals(TipoError.NO_ENCONTRADO, ajeno.getTipoError());
    }

    @Test void comprobanteDespuesSeRechazaVencidoOSinStock() {
        var registrado = registrar(2, "TRANSFERENCIA", false);
        var pedido = pedidos.get(0);
        var pdf = new ArchivoComprobante(PDF, "application/pdf", "pago.pdf");
        roja.setStock(1);
        var sinStock = assertThrows(NegocioException.class, () -> bo.subirComprobantePendiente(7L, registrado.folio(), pdf));
        assertTrue(sinStock.getMessage().contains("WhatsApp"), sinStock.getMessage());
        roja.setStock(8);
        pedido.setFechaHoraPedido(OffsetDateTime.now(RELOJ).minusHours(43));
        var vencido = assertThrows(NegocioException.class, () -> bo.subirComprobantePendiente(7L, registrado.folio(), pdf));
        assertTrue(vencido.getMessage().contains("42 horas"), vencido.getMessage());
        assertTrue(subidos.isEmpty());
        assertEquals(EstadoVenta.PENDIENTE_COMPROBANTE, pedido.getEstadoVenta());
    }

    @Test void confirmarTransferenciaDescuentaStockYPasaAPagado() {
        var registrado = registrar(2, "TRANSFERENCIA", true);
        var pagado = bo.confirmarPago(String.valueOf(registrado.id()), null);
        assertEquals("PAGADO", pagado.estadoVenta());
        assertEquals(6, roja.getStock());
        assertEquals(EstadoPago.COMPLETADO, pagos.get(0).getEstadoPago());
        assertEquals(LocalDate.now(RELOJ), pagos.get(0).getFechaPago());
        var repetido = assertThrows(NegocioException.class, () -> bo.confirmarPago(String.valueOf(registrado.id()), null));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, repetido.getTipoError());
        assertEquals(6, roja.getStock(), "Confirmar dos veces no descuenta dos veces");
    }

    @Test void confirmarEfectivoExigeCantidadRecibidaYCalculaElCambio() {
        var registrado = registrar(2, "EFECTIVO", false);
        var id = String.valueOf(registrado.id());
        assertThrows(NegocioException.class, () -> bo.confirmarPago(id, new ConfirmarPagoPeticionDTO(null)));
        assertThrows(NegocioException.class, () -> bo.confirmarPago(id, new ConfirmarPagoPeticionDTO(new BigDecimal("299.99"))));
        assertEquals(8, roja.getStock());
        var pagado = bo.confirmarPago(id, new ConfirmarPagoPeticionDTO(new BigDecimal("500")));
        assertEquals("PAGADO", pagado.estadoVenta());
        assertMonto("500.00", pagado.metodoPago().cantidadRecibida());
        assertMonto("200.00", pagado.metodoPago().cambio());
        assertEquals(6, roja.getStock());
    }

    @Test void confirmarSinStockSuficienteConservaElEstado() {
        var registrado = registrar(5, "TRANSFERENCIA", true);
        roja.setStock(3);
        var error = assertThrows(NegocioException.class, () -> bo.confirmarPago(String.valueOf(registrado.id()), null));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, error.getTipoError());
        assertTrue(error.getMessage().contains("necesita 5 y hay 3"), error.getMessage());
        assertEquals(3, roja.getStock());
        assertEquals(EstadoVenta.PAGO_EN_REVISION, pedidos.get(0).getEstadoVenta());
    }

    @Test void listaPorConfirmarEnOrdenFifoYEnlaceAlComprobante() {
        var efectivo = registrar(1, "EFECTIVO", false);
        var transferencia = registrar(1, "TRANSFERENCIA", true);
        var sinComprobante = registrar(1, "TRANSFERENCIA", false);
        // El comprobante llegó antes que el registro del pedido en efectivo: va primero.
        ((PagoTransferencia) pedidos.get(1).getPago()).setFechaHoraComprobante(OffsetDateTime.now(RELOJ).minusHours(2));
        var lista = bo.listarPorConfirmar();
        assertEquals(List.of(transferencia.id(), efectivo.id()), lista.stream().map(PedidoVentaDTO::id).toList());
        assertFalse(lista.stream().anyMatch(p -> p.id().equals(sinComprobante.id())), "Sin comprobante no hay nada que confirmar");
        var url = bo.obtenerUrlComprobante(String.valueOf(transferencia.id()));
        assertTrue(url.url().startsWith("https://firmado/pedidos/7/"));
        assertEquals(300, url.expiraEnSegundos());
        assertThrows(NegocioException.class, () -> bo.obtenerUrlComprobante(String.valueOf(efectivo.id())));
    }

    @Test void pedidosSinPagoVencidosSeCancelanYLiberanElCodigo() {
        bo.agregarAlCarrito(7L, new AgregarAlCarritoPeticionDTO(10L, 2));
        bo.aplicarCodigoDescuento(7L, new AplicarCodigoPeticionDTO("FLY10"));
        var pedido = carrito;
        pedido.setEstadoVenta(EstadoVenta.PENDIENTE_COMPROBANTE);
        fly10.setUsosActuales(1);
        vencidos = List.of(pedido);
        assertEquals(1, bo.cancelarPedidosVencidos());
        assertEquals(EstadoVenta.CANCELADO_VENCIMIENTO, pedido.getEstadoVenta());
        assertEquals(0, fly10.getUsosActuales());
    }
}
