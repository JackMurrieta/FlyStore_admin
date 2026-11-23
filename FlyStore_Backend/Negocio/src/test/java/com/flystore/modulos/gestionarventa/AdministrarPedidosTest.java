package com.flystore.modulos.gestionarventa;

import com.flystore.contratos.administrarpedidos.BuscarPedidosPeticionDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.ClienteBO;
import com.flystore.modulos.autenticacion.aplicacion.IAdminBO;
import com.flystore.modulos.autenticacion.aplicacion.IEmpleadoBO;
import com.flystore.modulos.gestionarventa.aplicacion.*;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoAdminMapper;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.DatosEnvio;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.IAlmacenComprobantes;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.INotificadorCliente;
import com.flystore.persistencia.consultas.CriteriosBusquedaPedidos;
import com.flystore.persistencia.consultas.PaginaPedidos;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CU-04 Administrar pedidos en PedidoVentaBO, sin base de datos: los DAO y los servicios externos
 * son dobles en memoria; HistorialPedidoBO, ClienteBO y VarianteProductoBO son los reales.
 */
class AdministrarPedidosTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-20T18:00:00Z"), ZoneId.of("America/Hermosillo"));
    private static final OffsetDateTime AHORA = OffsetDateTime.now(RELOJ);
    private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RUTA = "pedidos/7/FLY-000128/0f8fad5b-d9cb-469f-a165-70867728950e.jpg";
    private static final byte[] PDF = "%PDF-1.4 comprobante".getBytes(StandardCharsets.US_ASCII);

    private final Cliente ana = DatosPedidos.cliente(7, "Ana", "Torres", "644 123 4567");
    private final VarianteProducto gorra = DatosPedidos.variante(10, "9FORTY LA Dodgers", "Negro", "Ajustable", 8);
    private final List<PedidoVenta> pedidos = new ArrayList<>();
    private final List<HistorialPedido> historial = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private final List<String> textosBuscados = new ArrayList<>();
    private final List<String> subidos = new ArrayList<>();
    private List<Cliente> clientesEncontrados = List.of();
    private CriteriosBusquedaPedidos criteriosRecibidos;
    private List<Long> clientesRecibidos;
    private boolean notificadorDisponible = true;
    private boolean notificadorFalla;
    private boolean storageFalla;
    private int codigosLiberados;

    private final PedidoVentaBO bo = crearBO();

    private PedidoVentaBO crearBO() {
        var pedidoDAO = puerto(IPedidoVentaDAO.class, (m, a) -> switch (m) {
            case "bloquearPorFolio", "obtenerPorFolioCompleto" -> pedidos.stream().filter(p -> a[0].equals(p.getFolio())).findFirst();
            case "guardarPedidoVenta", "guardarPago" -> null;
            case "buscarPorCriterios" -> {
                criteriosRecibidos = (CriteriosBusquedaPedidos) a[0];
                clientesRecibidos = castIds(a[1]);
                yield new PaginaPedidos(pedidos, pedidos.size());
            }
            default -> throw new AssertionError("IPedidoVentaDAO." + m);
        });
        // Lo usa ClienteBO para buscar pedidos por cliente.
        var clienteDAO = puerto(IClienteDAO.class, (m, a) -> {
            if (!m.equals("buscarPorNombreOTelefono")) throw new AssertionError("IClienteDAO." + m);
            textosBuscados.add((String) a[0]);
            return clientesEncontrados;
        });
        var historialDAO = puerto(IHistorialPedidoDAO.class, (m, a) -> switch (m) {
            // La más reciente primero, como la consulta real.
            case "guardar" -> { historial.add(0, (HistorialPedido) a[0]); yield a[0]; }
            case "listarPorPedido" -> List.copyOf(historial);
            default -> throw new AssertionError("IHistorialPedidoDAO." + m);
        });
        var administrador = new Admin();
        administrador.setNombre("Freddy");
        administrador.setApellidos("Castro");
        var adminBO = puerto(IAdminBO.class, (m, a) -> ADMIN.equals(a[0]) ? Optional.of(administrador) : Optional.empty());
        var empleadoBO = puerto(IEmpleadoBO.class, (m, a) -> Optional.empty());
        var varianteDAO = puerto(IVarianteProductoDAO.class, (m, a) -> gorra.getId().equals(a[0]) ? Optional.of(gorra) : Optional.empty());
        var codigoBO = puerto(ICodigoDescuentoBO.class, (m, a) -> {
            if (!m.equals("liberarUso")) throw new AssertionError("ICodigoDescuentoBO." + m);
            codigosLiberados++;
            return null;
        });
        var detalleBO = puerto(IDetallePedidoBO.class, (m, a) -> {
            if (!m.equals("obtenerCodigosAplicados")) throw new AssertionError("IDetallePedidoBO." + m);
            return List.of();
        });
        var almacen = puerto(IAlmacenComprobantes.class, (m, a) -> switch (m) {
            case "subir" -> { subidos.add((String) a[0]); yield a[0]; }
            case "generarUrlTemporal" -> {
                if (storageFalla) throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "Storage no responde");
                yield "https://firmado/" + a[0] + "?expira=" + a[1];
            }
            default -> throw new AssertionError("IAlmacenComprobantes." + m);
        });
        INotificadorCliente notificador = (telefono, mensaje) -> {
            if (notificadorFalla) throw new IllegalStateException("sin red");
            avisos.add(telefono + " | " + mensaje);
            return notificadorDisponible;
        };
        var productoVentaBO = new ProductoVentaBO();
        return new PedidoVentaBO(pedidoDAO, clienteDAO, detalleBO, productoVentaBO, new VarianteProductoBO(varianteDAO), codigoBO,
                new ClienteBO(clienteDAO, new PedidoVentaMapper()), new HistorialPedidoBO(historialDAO, adminBO, empleadoBO, RELOJ),
                almacen, notificador, new PedidoVentaMapper(), new PedidoAdminMapper(), RELOJ, "52 662 123 4567",
                TransactionOperations.withoutTransaction());
    }

    @SuppressWarnings("unchecked")
    private static List<Long> castIds(Object valor) {
        return (List<Long>) valor;
    }

    private PedidoVenta transferenciaEnRevision(int piezas) {
        var pedido = DatosPedidos.pedido(128, ana, gorra, piezas, "210.00", EstadoVenta.PAGO_EN_REVISION, AHORA.minusHours(3));
        var pago = new PagoTransferencia();
        pago.setMonto(new BigDecimal("210.00").multiply(BigDecimal.valueOf(piezas)));
        pago.setEstadoPago(EstadoPago.EN_PROCESO);
        pago.setComprobantePagoUrl(RUTA);
        pago.setNumeroReferencia(pedido.getFolio());
        pago.setFechaHoraComprobante(AHORA.minusHours(2));
        pedido.setPago(pago);
        pedidos.add(pedido);
        return pedido;
    }

    private PedidoVenta apartadoEnEfectivo(int piezas, OffsetDateTime registrado) {
        var pedido = DatosPedidos.pedido(127, ana, gorra, piezas, "210.00", EstadoVenta.APARTADO_PENDIENTE_PAGO, registrado);
        var pago = new PagoEfectivo();
        pago.setMonto(new BigDecimal("210.00").multiply(BigDecimal.valueOf(piezas)));
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pedido.setPago(pago);
        pedidos.add(pedido);
        return pedido;
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " y fue " + real);
    }

    private static TipoError errorDe(org.junit.jupiter.api.function.Executable accion) {
        return assertThrows(NegocioException.class, accion).getTipoError();
    }

    // ===== Back 1 · Listar, buscar y detalle =====

    @Test void listaConvierteLosFiltrosYBuscaClientesSoloSiElTextoNoEsUnFolio() {
        transferenciaEnRevision(2);
        clientesEncontrados = List.of(ana);
        var pagina = bo.buscarPedidos(new BuscarPedidosPeticionDTO(" ana ", "pago_en_revision", "TRANSFERENCIA", "2026-11-19", "2026-11-20", 2));
        assertEquals(List.of("ana"), textosBuscados);
        assertEquals(List.of(7L), clientesRecibidos);
        assertEquals("ana", criteriosRecibidos.texto());
        assertEquals(EstadoVenta.PAGO_EN_REVISION, criteriosRecibidos.estado());
        assertEquals(CriteriosBusquedaPedidos.TRANSFERENCIA, criteriosRecibidos.metodoPago());
        assertEquals(OffsetDateTime.parse("2026-11-19T00:00:00-07:00"), criteriosRecibidos.desde());
        assertEquals(OffsetDateTime.parse("2026-11-21T00:00:00-07:00"), criteriosRecibidos.hasta(), "El día final entra completo");
        assertEquals(2, criteriosRecibidos.pagina());
        assertEquals(1, pagina.totalEncontrado());
        var renglon = pagina.pedidos().get(0);
        assertEquals("FLY-000128", renglon.folio());
        assertEquals("Ana Torres", renglon.clienteNombre());
        assertEquals("644 123 4567", renglon.clienteTelefono());
        assertEquals("TRANSFERENCIA", renglon.metodoPago());
        assertMonto("420.00", renglon.total());
        assertNull(renglon.fechaLimite(), "En revisión ya no corre el plazo");

        bo.buscarPedidos(new BuscarPedidosPeticionDTO("fly-0001", null, null, null, null, 0));
        assertEquals(1, textosBuscados.size(), "Un folio no se busca entre los clientes");
        assertNull(clientesRecibidos);
        assertEquals(1, criteriosRecibidos.pagina(), "La página mínima es 1");
    }

    @Test void listaRechazaFiltrosInvalidos() {
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.buscarPedidos(new BuscarPedidosPeticionDTO(null, "NO_EXISTE", null, null, null, 1))));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.buscarPedidos(new BuscarPedidosPeticionDTO(null, "PENDIENTE", null, null, null, 1))),
                "El carrito no es un pedido que administre el personal");
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.buscarPedidos(new BuscarPedidosPeticionDTO(null, null, "TARJETA", null, null, 1))));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.buscarPedidos(new BuscarPedidosPeticionDTO(null, null, null, "20/11/2026", null, 1))));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.buscarPedidos(new BuscarPedidosPeticionDTO(null, null, null, "2026-11-20", "2026-11-19", 1))));
    }

    @Test void detalleReuneClienteLineasComprobanteEHistorial() {
        var pedido = transferenciaEnRevision(2);
        pedido.getDetalles().get(0).setDescuentoUnitario(new BigDecimal("10.00"));
        var detalle = bo.obtenerDetallePedido(" fly-000128 ");
        assertEquals("PAGO_EN_REVISION", detalle.estado());
        assertEquals("2026-11-20T08:00:00-07:00", detalle.fechaCreacion());
        assertEquals(7L, detalle.cliente().id());
        assertEquals("Ana Torres", detalle.cliente().nombre());
        assertTrue(detalle.datosEntrega().contains("Calle Uno 123") && detalle.datosEntrega().contains("Ciudad Obregón"), detalle.datosEntrega());
        var linea = detalle.lineas().get(0);
        assertEquals("9FORTY LA Dodgers · Negro", linea.producto());
        assertEquals("Ajustable", linea.talla());
        assertEquals(2, linea.cantidad());
        assertMonto("200.00", linea.precioUnitario());
        assertMonto("400.00", linea.subtotal());
        assertMonto("420.00", detalle.total());
        assertEquals("TRANSFERENCIA", detalle.metodoPago());
        assertEquals("https://firmado/" + RUTA + "?expira=300", detalle.comprobante().url());
        assertEquals("0f8fad5b-d9cb-469f-a165-70867728950e.jpg", detalle.comprobante().nombreArchivo());
        assertEquals("EN_REVISION", detalle.comprobante().estado());
        assertEquals("FLY-000128", detalle.comprobante().concepto());
        assertNull(detalle.envio());
        assertTrue(detalle.historial().isEmpty());

        storageFalla = true;
        assertEquals("", bo.obtenerDetallePedido("FLY-000128").comprobante().url(), "Sin Storage el detalle se muestra igual, sin enlace");
    }

    @Test void detalleUsaElTelefonoDeLaEntregaSiLaCuentaNoTiene() {
        ana.setTelefono(null);
        transferenciaEnRevision(1);
        assertEquals("6449876543", bo.obtenerDetallePedido("FLY-000128").cliente().telefono());
    }

    @Test void folioInexistenteOCarritoRespondenNoEncontrado() {
        var carrito = DatosPedidos.pedido(130, ana, gorra, 1, "210.00", EstadoVenta.PENDIENTE, AHORA);
        pedidos.add(carrito);
        assertEquals(TipoError.NO_ENCONTRADO, errorDe(() -> bo.obtenerDetallePedido("FLY-000999")));
        assertEquals(TipoError.NO_ENCONTRADO, errorDe(() -> bo.obtenerDetallePedido("no-es-folio")));
        assertEquals(TipoError.NO_ENCONTRADO, errorDe(() -> bo.obtenerDetallePedido("FLY-000130")), "El carrito de un cliente no es un pedido");
        assertEquals(TipoError.NO_ENCONTRADO, errorDe(() -> bo.aprobarPago("FLY-000130", ADMIN)));
    }

    // ===== Back 2 · Registrar pago en efectivo =====

    @Test void pagoEnEfectivoValidaElMontoCalculaElCambioYNoAvisaAlCliente() {
        var pedido = apartadoEnEfectivo(2, AHORA.minusHours(20));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.registrarPagoEfectivo("FLY-000127", null, ADMIN)));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.registrarPagoEfectivo("FLY-000127", new BigDecimal("419.99"), ADMIN)));
        assertEquals(8, gorra.getStock());
        assertEquals(EstadoVenta.APARTADO_PENDIENTE_PAGO, pedido.getEstadoVenta());

        var resultado = bo.registrarPagoEfectivo("FLY-000127", new BigDecimal("500"), ADMIN);
        assertEquals("PAGADO", resultado.pedido().estado());
        assertNull(resultado.pedido().fechaLimite());
        assertEquals("EFECTIVO", resultado.pedido().metodoPago());
        assertEquals(6, gorra.getStock());
        var pago = (PagoEfectivo) pedido.getPago();
        assertMonto("500.00", pago.getCantidadRecibida());
        assertMonto("80.00", pago.getCambio());
        assertEquals(EstadoPago.COMPLETADO, pago.getEstadoPago());
        assertFalse(resultado.notificacionEnviada());
        assertTrue(avisos.isEmpty(), "Pagó en la tienda: no se le avisa");
        var entrada = resultado.pedido().historial().get(0);
        assertEquals("Freddy Castro", entrada.usuario());
        assertEquals("APARTADO_PENDIENTE_PAGO", entrada.estadoAnterior());
        assertEquals("PAGADO", entrada.estadoNuevo());
        assertEquals("pago en efectivo", entrada.motivo());
        assertEquals("2026-11-20T11:00:00-07:00", entrada.fechaHora());

        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.registrarPagoEfectivo("FLY-000127", new BigDecimal("500"), ADMIN)));
        assertEquals(6, gorra.getStock(), "Cobrar dos veces no descuenta dos veces");
    }

    @Test void apartadoVencidoYaNoSeCobra() {
        var pedido = apartadoEnEfectivo(1, AHORA.minusHours(43));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.registrarPagoEfectivo("FLY-000127", new BigDecimal("210"), ADMIN)));
        assertEquals(EstadoVenta.APARTADO_PENDIENTE_PAGO, pedido.getEstadoVenta());
        assertEquals(8, gorra.getStock());
        transferenciaEnRevision(1);
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.registrarPagoEfectivo("FLY-000128", new BigDecimal("210"), ADMIN)),
                "Una transferencia no se cobra en efectivo");
    }

    // ===== Back 3 · Validar pago =====

    @Test void aprobarDescuentaStockPasaAPreparacionYAvisaAlCliente() {
        var pedido = transferenciaEnRevision(2);
        var resultado = bo.aprobarPago("FLY-000128", ADMIN);
        assertEquals("PREPARACION", resultado.pedido().estado(), "Una transferencia aprobada entra directo a preparación");
        assertEquals("COMPLETADO", resultado.pedido().comprobante().estado());
        assertEquals(6, gorra.getStock());
        assertEquals(EstadoPago.COMPLETADO, pedido.getPago().getEstadoPago());
        assertEquals(AHORA.toLocalDate(), pedido.getPago().getFechaPago());
        assertTrue(resultado.notificacionEnviada());
        assertEquals(List.of("644 123 4567 | Hola Ana, te escribimos de FlyStore. Confirmamos el pago de tu pedido FLY-000128 y ya lo estamos preparando. ¡Gracias por tu compra!"), avisos);
        assertEquals("PAGO_EN_REVISION", resultado.pedido().historial().get(0).estadoAnterior());
        assertNull(resultado.pedido().historial().get(0).motivo());

        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.aprobarPago("FLY-000128", ADMIN)));
        assertEquals(6, gorra.getStock(), "Aprobar dos veces no descuenta dos veces");
    }

    @Test void aprobarSinStockSuficienteConservaElEstado() {
        var pedido = transferenciaEnRevision(5);
        gorra.setStock(3);
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.aprobarPago("FLY-000128", ADMIN)));
        assertEquals(3, gorra.getStock());
        assertEquals(EstadoVenta.PAGO_EN_REVISION, pedido.getEstadoVenta());
        assertTrue(historial.isEmpty() && avisos.isEmpty());
    }

    @Test void siElAvisoFallaElCambioSeConservaYSeOfreceAvisarAMano() {
        transferenciaEnRevision(1);
        notificadorFalla = true;
        var resultado = bo.aprobarPago("FLY-000128", ADMIN);
        assertEquals("PREPARACION", resultado.pedido().estado());
        assertFalse(resultado.notificacionEnviada());

        pedidos.clear();
        historial.clear();
        notificadorFalla = false;
        notificadorDisponible = false;
        transferenciaEnRevision(1);
        assertFalse(bo.aprobarPago("FLY-000128", null).notificacionEnviada(), "Sin proveedor de WhatsApp configurado");
        assertEquals("Personal de FlyStore", historial.get(0).getUsuario());
    }

    @Test void rechazarRegresaAPendienteDeComprobanteCon42HorasNuevasSinTocarStock() {
        var pedido = transferenciaEnRevision(2);
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.rechazarComprobante("FLY-000128", "  ", null, ADMIN)));
        var resultado = bo.rechazarComprobante("FLY-000128", " El monto no coincide ", "Faltan $90.", ADMIN);
        assertEquals("PENDIENTE_COMPROBANTE", resultado.pedido().estado());
        assertEquals("2026-11-22T05:00:00-07:00", resultado.pedido().fechaLimite());
        assertEquals(AHORA.plusHours(42), pedido.getFechaLimitePago());
        assertEquals("RECHAZADO", resultado.pedido().comprobante().estado());
        assertEquals(EstadoPago.PENDIENTE, pedido.getPago().getEstadoPago());
        assertEquals(8, gorra.getStock());
        assertEquals("El monto no coincide", resultado.pedido().historial().get(0).motivo());
        assertTrue(historial.get(0).isComprobanteRechazado());
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("(El monto no coincide). Faltan $90. Sube uno nuevo antes del 22 de noviembre a las 05:00"), avisos.get(0));

        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.rechazarComprobante("FLY-000128", "otra vez", null, ADMIN)));
    }

    @Test void trasElRechazoElClienteSubeOtroComprobanteConElPlazoNuevo() {
        var pedido = transferenciaEnRevision(1);
        // Registrado hace 60 h: sin el vencimiento nuevo, el plazo de 42 h ya habría pasado.
        pedido.setFechaHoraPedido(AHORA.minusHours(60));
        bo.rechazarComprobante("FLY-000128", "Ilegible", null, ADMIN);
        assertEquals("2026-11-22T05:00:00-07:00", bo.obtenerDetallePedido("FLY-000128").fechaLimite());

        var conNuevo = bo.subirComprobantePendiente(7L, "FLY-000128", new ArchivoComprobante(PDF, "application/pdf", "otro.pdf"));
        assertEquals("PAGO_EN_REVISION", conNuevo.estadoVenta());
        assertNull(conNuevo.fechaLimitePago(), "En revisión ya no corre el plazo");
        assertEquals(1, subidos.size());
        assertEquals(subidos.get(0), ((PagoTransferencia) pedido.getPago()).getComprobantePagoUrl());
        assertEquals(EstadoPago.EN_PROCESO, pedido.getPago().getEstadoPago());
        assertEquals("EN_REVISION", bo.obtenerDetallePedido("FLY-000128").comprobante().estado());
    }

    @Test void elPlazoNuevoTambienVence() {
        var pedido = transferenciaEnRevision(1);
        bo.rechazarComprobante("FLY-000128", "Ilegible", null, ADMIN);
        pedido.setFechaLimitePago(AHORA.minusMinutes(1));
        var vencido = assertThrows(NegocioException.class,
                () -> bo.subirComprobantePendiente(7L, "FLY-000128", new ArchivoComprobante(PDF, "application/pdf", "otro.pdf")));
        assertTrue(vencido.getMessage().contains("42 horas"), vencido.getMessage());
        assertTrue(subidos.isEmpty());
        assertEquals(EstadoVenta.PENDIENTE_COMPROBANTE, pedido.getEstadoVenta());
    }

    // ===== Back 4 · Actualizar estado =====

    @Test void laTransferenciaAvanzaDePreparacionAEnviadoYEntregado() {
        var pedido = transferenciaEnRevision(1);
        bo.aprobarPago("FLY-000128", ADMIN);
        avisos.clear();
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.actualizarEstado("FLY-000128", "ENTREGADO", null, ADMIN)), "No se salta ENVIADO");
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.actualizarEstado("FLY-000128", "EN_CAMINO", null, ADMIN)));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.actualizarEstado("FLY-000128", "ENVIADO", null, ADMIN)));
        assertEquals(TipoError.DATOS_INVALIDOS, errorDe(() -> bo.actualizarEstado("FLY-000128", "ENVIADO", new DatosEnvio("Estafeta", " ", false), ADMIN)));
        assertEquals(EstadoVenta.PREPARACION, pedido.getEstadoVenta());

        var enviado = bo.actualizarEstado("FLY-000128", "enviado", new DatosEnvio(" Estafeta ", "7210 4455", false), ADMIN);
        assertEquals("ENVIADO", enviado.pedido().estado());
        assertEquals("Estafeta", enviado.pedido().envio().paqueteria());
        assertEquals("7210 4455", enviado.pedido().envio().guia());
        assertFalse(enviado.pedido().envio().entregaLocal());
        assertTrue(avisos.get(0).endsWith("Ya enviamos tu pedido FLY-000128 por Estafeta, guía 7210 4455."), avisos.get(0));

        var entregado = bo.actualizarEstado("FLY-000128", "ENTREGADO", null, ADMIN);
        assertEquals("ENTREGADO", entregado.pedido().estado());
        assertEquals("Estafeta", entregado.pedido().envio().paqueteria(), "El envío se conserva al entregar");
        assertEquals(List.of("ENTREGADO", "ENVIADO", "PREPARACION"), entregado.pedido().historial().stream().map(h -> h.estadoNuevo()).toList());
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.actualizarEstado("FLY-000128", "ENTREGADO", null, ADMIN)));
        assertEquals(7, gorra.getStock(), "Avanzar el estado no vuelve a tocar el stock");
    }

    @Test void entregaLocalNoPidePaqueteriaNiGuia() {
        transferenciaEnRevision(1);
        bo.aprobarPago("FLY-000128", ADMIN);
        avisos.clear();
        var enviado = bo.actualizarEstado("FLY-000128", "ENVIADO", new DatosEnvio("Estafeta", null, true), ADMIN);
        assertTrue(enviado.pedido().envio().entregaLocal());
        assertNull(enviado.pedido().envio().paqueteria());
        assertTrue(avisos.get(0).endsWith("te lo entregaremos de forma local."), avisos.get(0));
    }

    @Test void elEfectivoCobradoPasaDePagadoAEntregadoSinEnvio() {
        apartadoEnEfectivo(1, AHORA.minusHours(2));
        bo.registrarPagoEfectivo("FLY-000127", new BigDecimal("210"), ADMIN);
        var error = assertThrows(NegocioException.class,
                () -> bo.actualizarEstado("FLY-000127", "ENVIADO", new DatosEnvio("Estafeta", "123", false), ADMIN));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, error.getTipoError());
        assertEquals("Desde «Pagado» solo se puede pasar a «Entregado».", error.getMessage());
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.actualizarEstado("FLY-000127", "PREPARACION", null, ADMIN)));

        var entregado = bo.actualizarEstado("FLY-000127", "ENTREGADO", null, ADMIN);
        assertEquals("ENTREGADO", entregado.pedido().estado());
        assertNull(entregado.pedido().envio());
        assertEquals(List.of("ENTREGADO", "PAGADO"), entregado.pedido().historial().stream().map(h -> h.estadoNuevo()).toList());
        assertTrue(avisos.get(0).endsWith("Registramos la entrega de tu pedido FLY-000127. ¡Gracias por tu compra!"), avisos.get(0));
    }

    @Test void losPedidosDeLaReglaAnteriorTambienAvanzan() {
        var transferencia = transferenciaEnRevision(1);
        transferencia.setEstadoVenta(EstadoVenta.PAGADO);
        var preparando = bo.actualizarEstado("FLY-000128", "PREPARACION", null, ADMIN);
        assertEquals("PREPARACION", preparando.pedido().estado());
        assertTrue(avisos.get(0).endsWith("Ya estamos preparando tu pedido FLY-000128."), avisos.get(0));

        var efectivo = apartadoEnEfectivo(1, AHORA.minusHours(2));
        efectivo.setEstadoVenta(EstadoVenta.ENVIADO);
        assertEquals("ENTREGADO", bo.actualizarEstado("FLY-000127", "ENTREGADO", null, ADMIN).pedido().estado());
    }

    // ===== Cancelación: solo el cliente, antes de pagar =====

    @Test void elClienteCancelaSuApartadoYSeLiberaElCodigo() {
        var pedido = apartadoEnEfectivo(2, AHORA.minusHours(5));
        var cancelado = bo.cancelarPedidoCliente(7L, "FLY-000127", "  Ya no lo necesito ");
        assertEquals("CANCELADO", cancelado.estadoVenta());
        assertNull(cancelado.fechaLimitePago());
        assertEquals(EstadoVenta.CANCELADO, pedido.getEstadoVenta());
        assertEquals(1, codigosLiberados);
        assertEquals(8, gorra.getStock(), "Antes del pago no había piezas descontadas");
        var entrada = historial.get(0);
        assertEquals("Ana Torres (cliente)", entrada.getUsuario());
        assertEquals(EstadoVenta.APARTADO_PENDIENTE_PAGO, entrada.getEstadoAnterior());
        assertEquals("Ya no lo necesito", entrada.getMotivo());
        assertTrue(avisos.isEmpty(), "El propio cliente canceló: no se le avisa");

        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.cancelarPedidoCliente(7L, "FLY-000127", null)));
        assertEquals(1, codigosLiberados, "Cancelar dos veces no libera dos veces");
    }

    @Test void elClienteCancelaSinMotivoMientrasEsperaElComprobante() {
        var pedido = transferenciaEnRevision(1);
        pedido.setEstadoVenta(EstadoVenta.PENDIENTE_COMPROBANTE);
        assertEquals("CANCELADO", bo.cancelarPedidoCliente(7L, "FLY-000128", "   ").estadoVenta());
        assertNull(historial.get(0).getMotivo());
    }

    @Test void pagadoEnRevisionVencidoOAjenoNoSeCancelan() {
        var enRevision = transferenciaEnRevision(1);
        assertEquals(TipoError.NO_ENCONTRADO, errorDe(() -> bo.cancelarPedidoCliente(99L, "FLY-000128", null)), "Un pedido ajeno es como si no existiera");
        assertEquals(TipoError.NO_ENCONTRADO, errorDe(() -> bo.cancelarPedidoCliente(7L, "no-es-folio", null)));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.cancelarPedidoCliente(7L, "FLY-000128", null)), "Con el comprobante en revisión ya no se cancela");
        enRevision.setEstadoVenta(EstadoVenta.PREPARACION);
        assertEquals(TipoError.CONFLICTO_DE_DATOS, errorDe(() -> bo.cancelarPedidoCliente(7L, "FLY-000128", null)), "Un pedido pagado no se cancela");

        var vencido = apartadoEnEfectivo(1, AHORA.minusHours(43));
        var error = assertThrows(NegocioException.class, () -> bo.cancelarPedidoCliente(7L, "FLY-000127", null));
        assertEquals(TipoError.CONFLICTO_DE_DATOS, error.getTipoError());
        assertTrue(error.getMessage().contains("se cancela solo"), error.getMessage());
        assertEquals(EstadoVenta.APARTADO_PENDIENTE_PAGO, vencido.getEstadoVenta());
        assertEquals(0, codigosLiberados);
        assertTrue(historial.isEmpty());
    }
}
