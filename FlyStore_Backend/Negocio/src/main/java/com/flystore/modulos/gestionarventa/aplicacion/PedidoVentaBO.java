package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.contratos.administrarpedidos.BuscarPedidosPeticionDTO;
import com.flystore.contratos.administrarpedidos.DetallePedidoAdminDTO;
import com.flystore.contratos.administrarpedidos.PaginaPedidosDTO;
import com.flystore.contratos.administrarpedidos.ResultadoAccionDTO;
import com.flystore.contratos.gestionarventa.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.IClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoAdminMapper;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.DatosEnvio;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.PedidoAdminCompleto;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ResultadoAccion;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.TotalesPedido;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.IAlmacenComprobantes;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.INotificadorCliente;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IPedidoVentaDAO;
import com.flystore.persistencia.entidades.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Coordina CU-02 Gestionar venta y CU-04 Administrar pedidos: todo lo que le pasa a un PedidoVenta,
 * desde el carrito hasta la entrega o la cancelación. Las reglas de líneas, precios, existencia,
 * códigos e historial están en sus propios BO; este BO decide el flujo, el estado del pedido y las
 * transacciones.
 *
 * <p>Las acciones del personal (CU-04) cambian el pedido dentro de una transacción y, ya confirmada,
 * avisan al cliente: si el aviso falla, el cambio no se revierte y la respuesta lleva
 * notificacion_enviada = false.
 */
@Service
public class PedidoVentaBO implements IPedidoVentaBO {

    private static final Logger logger = LoggerFactory.getLogger(PedidoVentaBO.class);

    /** Plazo para pagar o subir el comprobante. Al vencer, el pedido se cancela por vencimiento. */
    public static final Duration PLAZO_PAGO = Duration.ofHours(42);
    /** Estados de un pedido registrado que todavía no tiene pago ni comprobante. */
    public static final Set<EstadoVenta> ESTADOS_SIN_PAGO = Collections.unmodifiableSet(
            EnumSet.of(EstadoVenta.APARTADO_PENDIENTE_PAGO, EstadoVenta.PENDIENTE_COMPROBANTE));
    /** Estados que esperan que el personal confirme el pago: comprobante en revisión o efectivo pendiente. */
    public static final Set<EstadoVenta> ESTADOS_POR_CONFIRMAR = Collections.unmodifiableSet(
            EnumSet.of(EstadoVenta.PAGO_EN_REVISION, EstadoVenta.APARTADO_PENDIENTE_PAGO));
    /** Segundos que dura el enlace para ver un comprobante. */
    static final int VIGENCIA_URL_COMPROBANTE = 300;
    static final String TIPO_MIXTO = "MIXTO";
    static final String MOTIVO_PAGO_EFECTIVO = "pago en efectivo";

    private static final Pattern FOLIO = Pattern.compile("FLY-\\d{6,}");
    /** CU-04 · Un texto que empieza como folio se busca solo en el folio, sin consultar clientes. */
    private static final Pattern PARECE_FOLIO = Pattern.compile("(?i)fly[-\\d]*");
    /** Recorrido que el personal avanza a mano una vez pagado el pedido. */
    private static final Map<EstadoVenta, EstadoVenta> SIGUIENTE_ESTADO = Map.of(
            EstadoVenta.PAGADO, EstadoVenta.ENVIADO,
            EstadoVenta.PREPARACION, EstadoVenta.ENVIADO,
            EstadoVenta.ENVIADO, EstadoVenta.ENTREGADO);
    /** Estados en los que el pedido todavía no sale de la tienda y se puede cancelar. */
    private static final Set<EstadoVenta> CANCELABLES = EnumSet.of(EstadoVenta.APARTADO_PENDIENTE_PAGO,
            EstadoVenta.PENDIENTE_COMPROBANTE, EstadoVenta.PAGO_EN_REVISION, EstadoVenta.PAGADO, EstadoVenta.PREPARACION);
    /** Estados en los que el stock del pedido ya se descontó. */
    private static final Set<EstadoVenta> CON_STOCK_DESCONTADO = EnumSet.of(EstadoVenta.PAGADO, EstadoVenta.PREPARACION);
    /** Nombre de cada estado como lo ve el personal. */
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
    private static final DateTimeFormatter FECHA_AVISO =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'a las' HH:mm", Locale.forLanguageTag("es-MX"));
    private static final Map<String, String> EXTENSIONES = Map.of("image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");
    private static final String COMPROBANTE_INVALIDO = "Sube una imagen JPG o PNG, o un PDF, de máximo 5 MB.";

    private final IPedidoVentaDAO pedidoVentaDAO;
    private final IClienteDAO clienteDAO;
    private final IDetallePedidoBO detallePedidoBO;
    private final IProductoVentaBO productoVentaBO;
    private final IVarianteProductoBO varianteProductoBO;
    private final ICodigoDescuentoBO codigoDescuentoBO;
    private final IClienteBO clienteBO;
    private final IHistorialPedidoBO historialPedidoBO;
    private final IAlmacenComprobantes almacenComprobantes;
    private final INotificadorCliente notificador;
    private final PedidoVentaMapper mapper;
    private final PedidoAdminMapper adminMapper;
    private final Clock reloj;
    private final String telefonoWhatsapp;
    private final TransactionOperations transacciones;

    @Autowired
    public PedidoVentaBO(IPedidoVentaDAO pedidoVentaDAO, IClienteDAO clienteDAO, IDetallePedidoBO detallePedidoBO,
                         IProductoVentaBO productoVentaBO, IVarianteProductoBO varianteProductoBO,
                         ICodigoDescuentoBO codigoDescuentoBO, IClienteBO clienteBO, IHistorialPedidoBO historialPedidoBO,
                         IAlmacenComprobantes almacenComprobantes, INotificadorCliente notificador,
                         PedidoVentaMapper mapper, PedidoAdminMapper adminMapper, Clock reloj,
                         @Value("${app.ventas.whatsapp:}") String telefonoWhatsapp, PlatformTransactionManager gestorTransacciones) {
        this(pedidoVentaDAO, clienteDAO, detallePedidoBO, productoVentaBO, varianteProductoBO, codigoDescuentoBO, clienteBO,
                historialPedidoBO, almacenComprobantes, notificador, mapper, adminMapper, reloj, telefonoWhatsapp,
                new TransactionTemplate(gestorTransacciones));
    }

    /** Para pruebas: recibe directamente quién abre y cierra las transacciones de CU-04. */
    public PedidoVentaBO(IPedidoVentaDAO pedidoVentaDAO, IClienteDAO clienteDAO, IDetallePedidoBO detallePedidoBO,
                         IProductoVentaBO productoVentaBO, IVarianteProductoBO varianteProductoBO,
                         ICodigoDescuentoBO codigoDescuentoBO, IClienteBO clienteBO, IHistorialPedidoBO historialPedidoBO,
                         IAlmacenComprobantes almacenComprobantes, INotificadorCliente notificador,
                         PedidoVentaMapper mapper, PedidoAdminMapper adminMapper, Clock reloj,
                         String telefonoWhatsapp, TransactionOperations transacciones) {
        this.pedidoVentaDAO = pedidoVentaDAO;
        this.clienteDAO = clienteDAO;
        this.detallePedidoBO = detallePedidoBO;
        this.productoVentaBO = productoVentaBO;
        this.varianteProductoBO = varianteProductoBO;
        this.codigoDescuentoBO = codigoDescuentoBO;
        this.clienteBO = clienteBO;
        this.historialPedidoBO = historialPedidoBO;
        this.almacenComprobantes = almacenComprobantes;
        this.notificador = notificador;
        this.mapper = mapper;
        this.adminMapper = adminMapper;
        this.reloj = reloj;
        this.telefonoWhatsapp = telefonoWhatsapp == null ? "" : telefonoWhatsapp.replaceAll("\\D", "");
        this.transacciones = transacciones;
    }

    // ===== Carrito =====

    @Override
    @Transactional
    public PedidoVentaDTO obtenerPedidoEnCurso(Long clienteId) {
        var pedido = pedidoVentaDAO.bloquearPedidoPendienteCliente(clienteId);
        if (pedido.isEmpty()) return mapper.aPedidoVentaVacioDTO();
        // El carrito muestra siempre los precios y códigos vigentes.
        actualizarCarrito(pedido.get());
        return aPedidoVentaDTO(pedido.get());
    }

    @Override
    @Transactional
    public PedidoVentaDTO agregarAlCarrito(Long clienteId, AgregarAlCarritoPeticionDTO peticion) {
        var variante = varianteProductoBO.obtenerVarianteEnVenta(peticion.varianteProductoId());
        var pedido = obtenerOCrearCarrito(clienteId);
        int enCarrito = detallePedidoBO.buscarPorVariante(pedido, variante.getId()).map(DetallePedido::getCantidad).orElse(0);
        varianteProductoBO.validarExistencia(variante, enCarrito + peticion.cantidad());
        detallePedidoBO.agregarVariante(pedido, variante, peticion.cantidad());
        actualizarCarrito(pedido);
        return aPedidoVentaDTO(pedido);
    }

    @Override
    @Transactional
    public PedidoVentaDTO actualizarCantidad(Long clienteId, ActualizarCantidadPeticionDTO peticion) {
        var pedido = bloquearCarrito(clienteId);
        if (peticion.cantidad() > 0) {
            var linea = detallePedidoBO.buscarPorVariante(pedido, peticion.varianteProductoId())
                    .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Ese producto ya no está en tu carrito"));
            varianteProductoBO.validarExistencia(linea.getVariantes().get(0).getVarianteProducto(), peticion.cantidad());
        }
        detallePedidoBO.actualizarCantidad(pedido, peticion.varianteProductoId(), peticion.cantidad());
        actualizarCarrito(pedido);
        return aPedidoVentaDTO(pedido);
    }

    @Override
    @Transactional
    public PedidoVentaDTO aplicarCodigoDescuento(Long clienteId, AplicarCodigoPeticionDTO peticion) {
        var pedido = bloquearCarritoConProductos(clienteId);
        var codigo = codigoDescuentoBO.validarCodigo(peticion.codigo());
        int lineas = detallePedidoBO.aplicarCodigoDescuento(pedido, codigo, codigoDescuentoBO.obtenerProductosAplicables(codigo));
        if (lineas == 0)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El código no aplica a los productos de tu carrito");
        actualizarCarrito(pedido);
        return aPedidoVentaDTO(pedido);
    }

    // ===== Pago y registro =====

    @Override
    @Transactional
    public FolioDTO generarFolio(Long clienteId) {
        var pedido = bloquearCarritoConProductos(clienteId);
        if (pedido.getFolio() == null) {
            pedido.setFolio(formatearFolio(pedido.getId()));
            pedidoVentaDAO.guardarPedidoVenta(pedido);
        }
        return new FolioDTO(pedido.getFolio());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaValidacionComprobanteDTO subirComprobante(Long clienteId, String tipoPago, String folio, ArchivoComprobante archivo) {
        if (!"TRANSFERENCIA".equals(tipoPago))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El comprobante solo se sube en pago por transferencia");
        var pedido = pedidoVentaDAO.obtenerPedidoPendienteCliente(clienteId)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "No tienes un pedido en curso"));
        validarFolio(pedido, folio);
        var tipoContenido = validarComprobante(archivo);
        // Antes de pagar se avisa si ya no alcanzan las piezas, para que el cliente ajuste su carrito.
        varianteProductoBO.validarExistencia(pedido);
        var ruta = rutaComprobante(clienteId, folio) + UUID.randomUUID() + EXTENSIONES.get(tipoContenido);
        almacenComprobantes.subir(ruta, archivo.contenido(), tipoContenido);
        logger.info("Comprobante del pedido {} guardado en {}", folio, ruta);
        return new RespuestaValidacionComprobanteDTO(ruta, true, "Recibimos tu comprobante");
    }

    @Override
    @Transactional
    public PedidoVentaDTO registrarPedido(Long clienteId, RegistrarPedidoPeticionDTO peticion) {
        var pedido = bloquearCarritoConProductos(clienteId);
        var metodoPago = peticion.metodoPago();
        validarFolio(pedido, metodoPago.folio());

        actualizarCarrito(pedido);
        var totales = calcularTotales(pedido);
        if (peticion.total() != null && peticion.total().compareTo(totales.total()) != 0)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                    "El total de tu pedido cambió. Revisa tu carrito antes de confirmar.");
        varianteProductoBO.validarExistencia(pedido);

        var estado = determinarEstadoInicial(metodoPago);
        var pago = crearPago(clienteId, pedido, metodoPago, estado, totales.total());
        pedidoVentaDAO.guardarPago(pago);
        codigoDescuentoBO.registrarUso(detallePedidoBO.obtenerCodigosAplicados(pedido));
        // El stock no se descuenta aquí: solo cuando el personal confirma el pago (PAGADO).

        pedido.setDatosEntrega(clienteBO.registrarDatosEntrega(clienteId, peticion.datosEntrega()));
        pedido.setPago(pago);
        pedido.setEstadoVenta(estado);
        pedido.setFechaHoraPedido(OffsetDateTime.now(reloj));
        pedidoVentaDAO.guardarPedidoVenta(pedido);
        logger.info("Pedido {} registrado con estado {}", pedido.getFolio(), estado);
        return aPedidoVentaDTO(pedido, totales);
    }

    @Override
    @Transactional
    public PedidoVentaDTO subirComprobantePendiente(Long clienteId, String folio, ArchivoComprobante archivo) {
        var noEncontrado = new NegocioException(TipoError.NO_ENCONTRADO, "No encontramos ese pedido");
        if (folio == null || !FOLIO.matcher(folio).matches()) throw noEncontrado;
        var pedido = pedidoVentaDAO.bloquearPorFolio(folio)
                .filter(p -> p.getCliente().getId().equals(clienteId))
                .orElseThrow(() -> noEncontrado);
        if (pedido.getEstadoVenta() != EstadoVenta.PENDIENTE_COMPROBANTE || pedido.getPago() == null
                || !(pedido.getPago().sinProxy() instanceof PagoTransferencia transferencia))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Este pedido no está esperando un comprobante");
        var ahora = OffsetDateTime.now(reloj);
        var limite = calcularFechaLimite(pedido);
        if (limite != null && ahora.isAfter(limite))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El plazo de 42 horas para subir tu comprobante ya venció");
        var tipoContenido = validarComprobante(archivo);
        // Un pedido registrado ya no se puede editar: si faltan piezas, se avisa y lo resuelve la tienda.
        validarExistenciaPedidoRegistrado(pedido);

        var ruta = rutaComprobante(clienteId, folio) + UUID.randomUUID() + EXTENSIONES.get(tipoContenido);
        almacenComprobantes.subir(ruta, archivo.contenido(), tipoContenido);
        boolean compensaAlRevertir = retirarComprobanteSiSeRevierte(ruta);
        try {
            transferencia.setComprobantePagoUrl(ruta);
            transferencia.setEstadoPago(EstadoPago.EN_PROCESO);
            transferencia.setFechaHoraComprobante(ahora);
            pedido.setEstadoVenta(EstadoVenta.PAGO_EN_REVISION);
            pedidoVentaDAO.guardarPedidoVenta(pedido);
        } catch (RuntimeException e) {
            if (!compensaAlRevertir) retirarComprobante(ruta);
            throw e;
        }
        logger.info("Comprobante del pedido {} recibido después del registro", folio);
        return aPedidoVentaDTO(pedido);
    }

    // ===== Consultas de pedidos registrados =====

    @Override
    @Transactional(readOnly = true)
    public PedidoVentaDTO obtenerPedidoRegistrado(Long clienteId, String folio) {
        var noEncontrado = new NegocioException(TipoError.NO_ENCONTRADO, "No encontramos ese pedido");
        if (folio == null || !FOLIO.matcher(folio).matches()) throw noEncontrado;
        var pedido = pedidoVentaDAO.obtenerPorFolioCompleto(folio)
                .filter(p -> p.getCliente().getId().equals(clienteId) && p.getEstadoVenta() != EstadoVenta.PENDIENTE)
                .orElseThrow(() -> noEncontrado);
        return aPedidoVentaDTO(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoVentaDTO> listarPedidos(String rolUsuario, Long clienteId) {
        List<PedidoVenta> pedidos;
        if (esPersonal(rolUsuario)) pedidos = pedidoVentaDAO.listarRegistrados();
        else if ("CLIENTE".equalsIgnoreCase(rolUsuario) && clienteId != null) pedidos = pedidoVentaDAO.listarRegistradosPorCliente(clienteId);
        else throw new NegocioException(TipoError.ACCESO_DENEGADO, "No tienes permiso para ver pedidos");
        return pedidos.stream().map(this::aPedidoVentaDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoVentaDTO obtenerPedidoCompleto(String idPedido, String rolUsuario, Long clienteId) {
        var noEncontrado = new NegocioException(TipoError.NO_ENCONTRADO, "No se encontró el pedido con ID: " + idPedido);
        var pedido = pedidoVentaDAO.obtenerPorIdCompleto(PedidoVentaMapper.id(idPedido))
                .filter(p -> p.getEstadoVenta() != EstadoVenta.PENDIENTE)
                .orElseThrow(() -> noEncontrado);
        // Un cliente que pide un pedido ajeno recibe 404, igual que si no existiera.
        if (!esPersonal(rolUsuario) && !pedido.getCliente().getId().equals(clienteId)) throw noEncontrado;
        return aPedidoVentaDTO(pedido);
    }

    // ===== Pagos por confirmar (personal) =====

    @Override
    @Transactional(readOnly = true)
    public List<PedidoVentaDTO> listarPorConfirmar() {
        return pedidoVentaDAO.listarPorEstados(ESTADOS_POR_CONFIRMAR).stream()
                .sorted(Comparator.comparing(PedidoVentaBO::llegada, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PedidoVenta::getId))
                .map(this::aPedidoVentaDTO)
                .toList();
    }

    @Override
    @Transactional
    public int cancelarPedidosVencidos() {
        var ahora = OffsetDateTime.now(reloj);
        // El plazo corre desde el registro o, si el personal rechazó un comprobante, desde el vencimiento nuevo.
        var vencidos = pedidoVentaDAO.listarPorEstadosRegistradosAntesDe(ESTADOS_SIN_PAGO, ahora.minus(PLAZO_PAGO), ahora);
        for (var pedido : vencidos) {
            codigoDescuentoBO.liberarUso(detallePedidoBO.obtenerCodigosAplicados(pedido));
            pedido.setEstadoVenta(EstadoVenta.CANCELADO_VENCIMIENTO);
            logger.info("Pedido {} cancelado por vencimiento", pedido.getFolio());
        }
        return vencidos.size();
    }

    // ===== CU-04 Administrar pedidos (personal) =====

    @Override
    @Transactional(readOnly = true)
    public PaginaPedidosDTO buscarPedidos(BuscarPedidosPeticionDTO peticion) {
        var criterios = adminMapper.toCriterios(peticion, reloj.getZone());
        // Si el texto no es un folio, puede ser el nombre o el teléfono de un cliente.
        List<Long> clienteIds = criterios.texto() == null || PARECE_FOLIO.matcher(criterios.texto()).matches() ? null
                : clienteBO.buscarIdsPorNombreOTelefono(criterios.texto());
        return adminMapper.toPaginaDTO(pedidoVentaDAO.buscarPorCriterios(criterios, clienteIds), this::calcularFechaLimite);
    }

    @Override
    @Transactional(readOnly = true)
    public DetallePedidoAdminDTO obtenerDetallePedido(String folio) {
        var pedido = pedidoVentaDAO.obtenerPorFolioCompleto(folioRegistrado(folio))
                .filter(p -> p.getEstadoVenta() != EstadoVenta.PENDIENTE)
                .orElseThrow(PedidoVentaBO::pedidoNoEncontrado);
        return detalleAdmin(pedido);
    }

    @Override
    public ResultadoAccionDTO registrarPagoEfectivo(String folio, BigDecimal montoRecibido, UUID adminId) {
        return ejecutarAccion(() -> {
            var pedido = bloquearRegistrado(folio);
            // Vencido aunque la tarea de vencimiento aún no lo haya cancelado: ya no se cobra.
            if (pedido.getEstadoVenta() != EstadoVenta.APARTADO_PENDIENTE_PAGO || estaVencido(pedido, OffsetDateTime.now(reloj))
                    || !(pago(pedido) instanceof PagoEfectivo efectivo))
                throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                        "El apartado venció o el pedido cambió de estado. El cliente debe hacer un nuevo pedido.");
            var cambio = calcularCambio(valor(efectivo.getMonto()), montoRecibido);
            // Primero el stock: si no alcanza se lanza CONFLICTO_DE_DATOS y el pedido conserva su estado.
            varianteProductoBO.descontarExistencia(pedido);
            efectivo.setCantidadRecibida(montoRecibido);
            efectivo.setCambio(cambio);
            efectivo.setEstadoPago(EstadoPago.COMPLETADO);
            efectivo.setFechaPago(LocalDate.now(reloj));
            cambiarEstado(pedido, EstadoVenta.PAGADO, adminId, MOTIVO_PAGO_EFECTIVO);
            // El cliente pagó en la tienda: no hace falta avisarle.
            return resultadoAccion(pedido, null);
        });
    }

    @Override
    public ResultadoAccionDTO aprobarPago(String folio, UUID adminId) {
        return ejecutarAccion(() -> {
            var pedido = bloquearRegistrado(folio);
            exigirEstado(pedido, EstadoVenta.PAGO_EN_REVISION);
            var transferencia = pagoTransferencia(pedido);
            // Primero el stock: si no alcanza se lanza CONFLICTO_DE_DATOS y el pedido conserva su estado.
            varianteProductoBO.descontarExistencia(pedido);
            transferencia.setEstadoPago(EstadoPago.COMPLETADO);
            transferencia.setFechaPago(LocalDate.now(reloj));
            cambiarEstado(pedido, EstadoVenta.PAGADO, adminId, null);
            return resultadoAccion(pedido, mensajePagoValidado(pedido));
        });
    }

    @Override
    public ResultadoAccionDTO rechazarComprobante(String folio, String motivo, String mensaje, UUID adminId) {
        var motivoLimpio = motivoObligatorio(motivo, "Indica el motivo del rechazo.");
        return ejecutarAccion(() -> {
            var pedido = bloquearRegistrado(folio);
            exigirEstado(pedido, EstadoVenta.PAGO_EN_REVISION);
            var transferencia = pagoTransferencia(pedido);
            // El archivo rechazado no se borra y el stock no se toca: nunca se descontó.
            transferencia.setEstadoPago(EstadoPago.PENDIENTE);
            var nuevoLimite = nuevoLimiteComprobante(OffsetDateTime.now(reloj));
            var anterior = pedido.getEstadoVenta();
            pedido.setEstadoVenta(EstadoVenta.PENDIENTE_COMPROBANTE);
            pedido.setFechaLimitePago(nuevoLimite);
            pedidoVentaDAO.guardarPedidoVenta(pedido);
            historialPedidoBO.registrarCambio(pedido, anterior, adminId, motivoLimpio, true);
            return resultadoAccion(pedido, mensajeComprobanteRechazado(pedido, motivoLimpio, mensaje, nuevoLimite));
        });
    }

    @Override
    public ResultadoAccionDTO actualizarEstado(String folio, String estado, DatosEnvio envio, UUID adminId) {
        var nuevo = estadoValido(estado);
        return ejecutarAccion(() -> {
            var pedido = bloquearRegistrado(folio);
            validarTransicion(pedido.getEstadoVenta(), nuevo);
            validarDatosEnvio(nuevo, envio);
            if (nuevo == EstadoVenta.ENVIADO) {
                // Con entrega local no hay paquetería ni guía.
                pedido.setPaqueteria(envio.entregaLocal() ? null : envio.paqueteria());
                pedido.setGuia(envio.entregaLocal() ? null : envio.guia());
                pedido.setEntregaLocal(envio.entregaLocal());
            }
            cambiarEstado(pedido, nuevo, adminId, null);
            return resultadoAccion(pedido, mensajeCambioEstado(pedido));
        });
    }

    @Override
    public ResultadoAccionDTO cancelarPedido(String folio, String motivo, UUID adminId) {
        var motivoLimpio = motivoObligatorio(motivo, "Indica el motivo de la cancelación.");
        return ejecutarAccion(() -> {
            var pedido = bloquearRegistrado(folio);
            var estado = pedido.getEstadoVenta();
            if (!puedeCancelarse(estado))
                throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                        estado == EstadoVenta.CANCELADO || estado == EstadoVenta.CANCELADO_VENCIMIENTO
                                ? "El pedido ya está cancelado."
                                : "El pedido ya salió de la tienda y no se puede cancelar.");
            // Solo un pedido ya pagado tiene piezas descontadas que devolver al inventario.
            if (CON_STOCK_DESCONTADO.contains(estado)) varianteProductoBO.reponerExistencia(pedido);
            // Igual que al vencer: el código de descuento vuelve a estar disponible.
            codigoDescuentoBO.liberarUso(detallePedidoBO.obtenerCodigosAplicados(pedido));
            cambiarEstado(pedido, EstadoVenta.CANCELADO, adminId, motivoLimpio);
            return resultadoAccion(pedido, mensajeCancelacion(pedido, motivoLimpio));
        });
    }

    // ===== Reglas propias del pedido =====

    /** Estado con el que nace el pedido según la forma de pago. */
    EstadoVenta determinarEstadoInicial(MetodoPagoDTO metodoPago) {
        if ("EFECTIVO".equals(metodoPago.tipoPago())) return EstadoVenta.APARTADO_PENDIENTE_PAGO;
        return tieneTexto(metodoPago.comprobanteRuta()) ? EstadoVenta.PAGO_EN_REVISION : EstadoVenta.PENDIENTE_COMPROBANTE;
    }

    /**
     * Vencimiento de un pedido registrado sin pago; null en los demás estados. Si el personal
     * rechazó su comprobante (CU-04), manda el vencimiento nuevo guardado en el pedido.
     */
    OffsetDateTime calcularFechaLimite(PedidoVenta pedido) {
        if (!ESTADOS_SIN_PAGO.contains(pedido.getEstadoVenta())) return null;
        if (pedido.getFechaLimitePago() != null) return pedido.getFechaLimitePago();
        return pedido.getFechaHoraPedido() != null ? pedido.getFechaHoraPedido().plus(PLAZO_PAGO) : null;
    }

    /** El pedido sigue sin pago y su plazo ya pasó. */
    boolean estaVencido(PedidoVenta pedido, OffsetDateTime ahora) {
        var limite = calcularFechaLimite(pedido);
        return limite != null && ahora.isAfter(limite);
    }

    /** CU-04 · Vencimiento que recibe el pedido cuando se rechaza su comprobante: 42 h desde ahora. */
    OffsetDateTime nuevoLimiteComprobante(OffsetDateTime ahora) {
        return ahora.plus(PLAZO_PAGO);
    }

    /** CU-04 · Cobro en efectivo: lo recibido debe cubrir el total; devuelve el cambio. */
    BigDecimal calcularCambio(BigDecimal total, BigDecimal montoRecibido) {
        if (montoRecibido == null)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Captura el monto recibido en efectivo.");
        if (montoRecibido.compareTo(total) < 0)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El monto recibido es menor al total del pedido ($"
                    + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + ").");
        return montoRecibido.subtract(total).setScale(2, RoundingMode.HALF_UP);
    }

    /** Nombre del estado como lo ve el personal. */
    String etiqueta(EstadoVenta estado) {
        return ETIQUETAS.getOrDefault(estado, String.valueOf(estado));
    }

    /**
     * CU-04 · Lanza CONFLICTO_DE_DATOS si el pedido ya no está en el estado que la acción necesita:
     * otra persona lo cambió mientras se tenía abierto y hay que volver a cargarlo.
     */
    void exigirEstado(PedidoVenta pedido, EstadoVenta esperado) {
        if (pedido.getEstadoVenta() != esperado)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS, "El pedido " + pedido.getFolio() + " ya no está en «"
                    + etiqueta(esperado) + "»: ahora está en «" + etiqueta(pedido.getEstadoVenta()) + "». Vuelve a cargarlo.");
    }

    /** CU-04 · Lanza CONFLICTO_DE_DATOS si el cambio se salta la secuencia Pagado → Enviado → Entregado. */
    void validarTransicion(EstadoVenta actual, EstadoVenta nuevo) {
        var siguiente = SIGUIENTE_ESTADO.get(actual);
        if (siguiente == null)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                    "Un pedido en «" + etiqueta(actual) + "» no admite cambio de estado.");
        if (siguiente != nuevo)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                    "Desde «" + etiqueta(actual) + "» solo se puede pasar a «" + etiqueta(siguiente) + "».");
    }

    /** CU-04 · Para marcar ENVIADO hacen falta paquetería y guía, o entrega local. Lanza DATOS_INVALIDOS si faltan. */
    void validarDatosEnvio(EstadoVenta nuevo, DatosEnvio envio) {
        if (nuevo == EstadoVenta.ENVIADO && (envio == null || !envio.completo()))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Indica paquetería y número de guía, o marca entrega local.");
    }

    /** CU-04 · Un pedido se puede cancelar mientras no haya salido de la tienda ni esté ya cancelado. */
    boolean puedeCancelarse(EstadoVenta estado) {
        return CANCELABLES.contains(estado);
    }

    /** Totales del pedido con los precios ya guardados en sus líneas. */
    TotalesPedido calcularTotales(PedidoVenta pedido) {
        var lineas = pedido.getDetalles() == null ? List.<DetallePedido>of() : pedido.getDetalles();
        var preciosBase = new LinkedHashMap<Long, BigDecimal>();
        var productos = new LinkedHashMap<Long, Producto>();
        var tipos = new TreeSet<String>();
        int totalPiezas = 0;
        var subtotalBase = BigDecimal.ZERO;
        var ahorro = BigDecimal.ZERO;
        var descuentos = BigDecimal.ZERO;
        for (var detalle : lineas) {
            var producto = detalle.getVariantes().get(0).getVarianteProducto().getProducto();
            var precio = valor(detalle.getPrecioUnitario());
            var base = preciosBase.computeIfAbsent(producto.getId(), id -> precioBase(producto, precio));
            var cantidad = BigDecimal.valueOf(detalle.getCantidad());
            productos.putIfAbsent(producto.getId(), producto);
            if (detalle.getTipoPrecio() != null) tipos.add(detalle.getTipoPrecio().name());
            totalPiezas += detalle.getCantidad();
            subtotalBase = subtotalBase.add(base.multiply(cantidad));
            ahorro = ahorro.add(base.subtract(precio).multiply(cantidad));
            descuentos = descuentos.add(valor(detalle.getDescuentoUnitario()).multiply(cantidad));
        }
        String tipoAplicado = tipos.isEmpty() ? TipoPrecio.BASE.name() : tipos.size() == 1 ? tipos.first() : TIPO_MIXTO;
        String siguiente = null;
        Integer faltantes = null;
        List<PrecioProducto> niveles = List.of();
        // El avance al siguiente esquema solo tiene sentido en el carrito y con un único producto.
        if (pedido.getEstadoVenta() == EstadoVenta.PENDIENTE && productos.size() == 1) {
            var producto = productos.values().iterator().next();
            niveles = productoVentaBO.obtenerNivelesPrecio(producto);
            var proximo = productoVentaBO.obtenerSiguienteTipoPrecio(producto, totalPiezas);
            if (proximo.isPresent()) {
                siguiente = proximo.get().getTipoPrecio().name();
                faltantes = proximo.get().getMinPiezas() - totalPiezas;
            }
        }
        var envio = valor(pedido.getCostoEnvio());
        var total = subtotalBase.subtract(ahorro).subtract(descuentos).add(envio);
        return new TotalesPedido(totalPiezas, tipoAplicado, siguiente, faltantes, niveles, subtotalBase, ahorro,
                descuentos, envio, total, preciosBase);
    }

    // ===== Avisos al cliente (CU-04) =====

    String mensajePagoValidado(PedidoVenta pedido) {
        return saludo(pedido) + " Confirmamos el pago de tu pedido " + pedido.getFolio() + ". ¡Gracias por tu compra!";
    }

    /** {@code mensaje} es el texto opcional que escribió el personal; puede ser null. */
    String mensajeComprobanteRechazado(PedidoVenta pedido, String motivo, String mensaje, OffsetDateTime nuevoLimite) {
        var texto = new StringBuilder(saludo(pedido)).append(" No pudimos validar el comprobante de tu pedido ")
                .append(pedido.getFolio()).append(" (").append(motivo.trim()).append(").");
        if (tieneTexto(mensaje)) texto.append(' ').append(mensaje.trim());
        return texto.append(" Sube uno nuevo antes del ").append(FECHA_AVISO.format(nuevoLimite))
                .append(" para conservar tu apartado.").toString();
    }

    /** Aviso de ENVIADO (con paquetería y guía, o entrega local) o de ENTREGADO. */
    String mensajeCambioEstado(PedidoVenta pedido) {
        var inicio = saludo(pedido);
        if (pedido.getEstadoVenta() == EstadoVenta.ENTREGADO)
            return inicio + " Registramos la entrega de tu pedido " + pedido.getFolio() + ". ¡Gracias por tu compra!";
        if (pedido.getGuia() == null || Boolean.TRUE.equals(pedido.getEntregaLocal()))
            return inicio + " Ya salió tu pedido " + pedido.getFolio() + " y te lo entregaremos de forma local.";
        return inicio + " Ya enviamos tu pedido " + pedido.getFolio() + " por " + pedido.getPaqueteria() + ", guía " + pedido.getGuia() + ".";
    }

    String mensajeCancelacion(PedidoVenta pedido, String motivo) {
        return saludo(pedido) + " Cancelamos tu pedido " + pedido.getFolio() + ": " + motivo.trim() + ".";
    }

    private static String saludo(PedidoVenta pedido) {
        var cliente = pedido.getCliente();
        var nombre = cliente == null || cliente.getNombre() == null ? "" : cliente.getNombre().trim();
        return (nombre.isEmpty() ? "Hola" : "Hola " + nombre) + ", te escribimos de FlyStore.";
    }

    // ===== Auxiliares de CU-04 =====

    /** Ejecuta la acción en una transacción y, ya confirmada, avisa al cliente. */
    private ResultadoAccionDTO ejecutarAccion(Supplier<ResultadoAccion> accion) {
        var resultado = transacciones.execute(estado -> accion.get());
        return adminMapper.toResultadoDTO(resultado, notificar(resultado));
    }

    /** Pedido registrado con bloqueo de escritura: dos personas no lo cambian a la vez. */
    private PedidoVenta bloquearRegistrado(String folio) {
        return pedidoVentaDAO.bloquearPorFolio(folioRegistrado(folio))
                .filter(p -> p.getEstadoVenta() != EstadoVenta.PENDIENTE)
                .orElseThrow(PedidoVentaBO::pedidoNoEncontrado);
    }

    /** Cambia el estado, guarda el pedido y deja la entrada en el historial. */
    private void cambiarEstado(PedidoVenta pedido, EstadoVenta nuevo, UUID adminId, String motivo) {
        var anterior = pedido.getEstadoVenta();
        pedido.setEstadoVenta(nuevo);
        pedidoVentaDAO.guardarPedidoVenta(pedido);
        historialPedidoBO.registrarCambio(pedido, anterior, adminId, motivo, false);
    }

    /** Avisa al cliente. Nunca lanza: un aviso fallido no deshace el cambio ya confirmado. */
    boolean notificar(ResultadoAccion resultado) {
        if (resultado.mensaje() == null || resultado.telefono() == null) return false;
        try {
            return notificador.notificar(resultado.telefono(), resultado.mensaje());
        } catch (RuntimeException e) {
            logger.warn("No se pudo avisar al cliente del pedido {}", resultado.pedido().folio(), e);
            return false;
        }
    }

    private ResultadoAccion resultadoAccion(PedidoVenta pedido, String mensaje) {
        return new ResultadoAccion(detalleAdmin(pedido), adminMapper.telefonoCliente(pedido), mensaje);
    }

    private DetallePedidoAdminDTO detalleAdmin(PedidoVenta pedido) {
        var completo = new PedidoAdminCompleto(pedido, historialPedidoBO.listarPorPedido(pedido.getId()), urlComprobante(pedido));
        return adminMapper.toDetalleDTO(completo, calcularFechaLimite(pedido));
    }

    /** Enlace temporal al comprobante; vacío si no hay o Storage no respondió (el detalle se muestra igual). */
    private String urlComprobante(PedidoVenta pedido) {
        if (!(pago(pedido) instanceof PagoTransferencia transferencia) || !tieneTexto(transferencia.getComprobantePagoUrl())) return "";
        try {
            return almacenComprobantes.generarUrlTemporal(transferencia.getComprobantePagoUrl(), VIGENCIA_URL_COMPROBANTE);
        } catch (RuntimeException e) {
            logger.warn("No se pudo generar el enlace al comprobante del pedido {}", pedido.getFolio());
            return "";
        }
    }

    private PagoTransferencia pagoTransferencia(PedidoVenta pedido) {
        if (pago(pedido) instanceof PagoTransferencia transferencia) return transferencia;
        throw new NegocioException(TipoError.CONFLICTO_DE_DATOS, "El pedido " + pedido.getFolio() + " no se pagó por transferencia.");
    }

    /** Pago real del pedido (sin el proxy de Hibernate); null si no tiene. */
    private static Pago pago(PedidoVenta pedido) {
        return pedido.getPago() == null ? null : pedido.getPago().sinProxy();
    }

    /** Folio escrito por el personal: sin espacios y en mayúsculas. Si no tiene forma de folio, 404. */
    private static String folioRegistrado(String folio) {
        var limpio = folio == null ? "" : folio.trim().toUpperCase(Locale.ROOT);
        if (!FOLIO.matcher(limpio).matches()) throw pedidoNoEncontrado();
        return limpio;
    }

    private static EstadoVenta estadoValido(String estado) {
        try {
            return EstadoVenta.valueOf(estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Estado de pedido inválido.");
        }
    }

    private static String motivoObligatorio(String motivo, String mensaje) {
        if (!tieneTexto(motivo)) throw new NegocioException(TipoError.DATOS_INVALIDOS, mensaje);
        return motivo.trim();
    }

    private static NegocioException pedidoNoEncontrado() {
        return new NegocioException(TipoError.NO_ENCONTRADO, "No existe un pedido con ese folio.");
    }

    // ===== Auxiliares =====

    private PedidoVenta obtenerOCrearCarrito(Long clienteId) {
        return pedidoVentaDAO.bloquearPedidoPendienteCliente(clienteId).orElseGet(() -> {
            // Bloquear al cliente serializa la creación: una segunda petición simultánea espera aquí
            // y, al continuar, encuentra el carrito que creó la primera.
            clienteDAO.bloquearPorId(clienteId)
                    .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Cliente no encontrado"));
            return pedidoVentaDAO.bloquearPedidoPendienteCliente(clienteId)
                    .orElseGet(() -> pedidoVentaDAO.crearPedidoVenta(clienteId));
        });
    }

    private PedidoVenta bloquearCarrito(Long clienteId) {
        return pedidoVentaDAO.bloquearPedidoPendienteCliente(clienteId)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Tu carrito está vacío"));
    }

    private PedidoVenta bloquearCarritoConProductos(Long clienteId) {
        var pedido = bloquearCarrito(clienteId);
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty())
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Tu carrito está vacío");
        return pedido;
    }

    /** Quita códigos que dejaron de ser válidos, recalcula precios y guarda. */
    private void actualizarCarrito(PedidoVenta pedido) {
        var hoy = LocalDate.now(reloj);
        for (var codigo : List.copyOf(detallePedidoBO.obtenerCodigosAplicados(pedido))) {
            if (!codigoDescuentoBO.esVigente(codigo, hoy)) detallePedidoBO.quitarCodigoDescuento(pedido, codigo);
        }
        detallePedidoBO.recalcularPrecios(pedido);
        pedidoVentaDAO.guardarPedidoVenta(pedido);
    }

    private Pago crearPago(Long clienteId, PedidoVenta pedido, MetodoPagoDTO metodoPago, EstadoVenta estado, BigDecimal total) {
        Pago pago;
        if ("EFECTIVO".equals(metodoPago.tipoPago())) {
            pago = new PagoEfectivo();
            pago.setEstadoPago(EstadoPago.PENDIENTE);
        } else {
            var transferencia = new PagoTransferencia();
            transferencia.setNumeroReferencia(pedido.getFolio());
            if (estado == EstadoVenta.PAGO_EN_REVISION) {
                var ruta = metodoPago.comprobanteRuta().trim();
                if (!ruta.matches(Pattern.quote(rutaComprobante(clienteId, pedido.getFolio())) + "[0-9a-f-]{36}\\.(jpg|png|pdf)"))
                    throw new NegocioException(TipoError.DATOS_INVALIDOS, "El comprobante no corresponde a tu pedido. Vuelve a subirlo.");
                transferencia.setComprobantePagoUrl(ruta);
                transferencia.setEstadoPago(EstadoPago.EN_PROCESO);
                transferencia.setFechaHoraComprobante(OffsetDateTime.now(reloj));
            } else {
                transferencia.setEstadoPago(EstadoPago.PENDIENTE);
            }
            pago = transferencia;
        }
        pago.setMonto(total);
        return pago;
    }

    /** Comprueba formato y tamaño por el contenido del archivo; devuelve su tipo MIME. */
    private String validarComprobante(ArchivoComprobante archivo) {
        if (archivo == null || archivo.contenido() == null || archivo.contenido().length == 0)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Selecciona tu comprobante");
        var datos = archivo.contenido();
        if (datos.length > ArchivoComprobante.MAX_BYTES) throw new NegocioException(TipoError.DATOS_INVALIDOS, COMPROBANTE_INVALIDO);
        if (empiezaCon(datos, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (empiezaCon(datos, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
        if (empiezaCon(datos, '%', 'P', 'D', 'F', '-')) return "application/pdf";
        throw new NegocioException(TipoError.DATOS_INVALIDOS, COMPROBANTE_INVALIDO);
    }

    /** Orden FIFO: hora de llegada del comprobante; en efectivo, la hora de registro del pedido. */
    private static OffsetDateTime llegada(PedidoVenta pedido) {
        if (pedido.getPago() != null && pedido.getPago().sinProxy() instanceof PagoTransferencia transferencia
                && transferencia.getFechaHoraComprobante() != null)
            return transferencia.getFechaHoraComprobante();
        return pedido.getFechaHoraPedido();
    }

    /** Como validarExistencia, pero el mensaje pide contactar a la tienda: el pedido ya no se puede editar. */
    private void validarExistenciaPedidoRegistrado(PedidoVenta pedido) {
        try {
            varianteProductoBO.validarExistencia(pedido);
        } catch (NegocioException e) {
            throw new NegocioException(e.getTipoError(), e.getMessage() + ". Escríbenos por WhatsApp para ajustar tu pedido antes de pagar.");
        }
    }

    /**
     * Si hay transacción, borra el archivo cuando se revierte. Devuelve false si no hay transacción
     * (por ejemplo, en pruebas) y la limpieza queda a cargo de quien llama.
     */
    private boolean retirarComprobanteSiSeRevierte(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return false;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int estado) {
                if (estado == STATUS_ROLLED_BACK) retirarComprobante(ruta);
            }
        });
        return true;
    }

    private void retirarComprobante(String ruta) {
        try {
            almacenComprobantes.eliminar(ruta);
        } catch (RuntimeException e) {
            logger.error("No se pudo retirar el comprobante {} tras un error; requiere limpieza", ruta);
        }
    }

    private void validarFolio(PedidoVenta pedido, String folio) {
        if (pedido.getFolio() == null || !pedido.getFolio().equals(folio))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El folio no corresponde a tu pedido en curso");
    }

    private PedidoVentaDTO aPedidoVentaDTO(PedidoVenta pedido) {
        return aPedidoVentaDTO(pedido, calcularTotales(pedido));
    }

    private PedidoVentaDTO aPedidoVentaDTO(PedidoVenta pedido, TotalesPedido totales) {
        return mapper.aPedidoVentaDTO(pedido, totales, calcularFechaLimite(pedido), enlaceWhatsapp(pedido));
    }

    private String enlaceWhatsapp(PedidoVenta pedido) {
        if (telefonoWhatsapp.isEmpty() || pedido.getFolio() == null || pedido.getEstadoVenta() == EstadoVenta.PENDIENTE) return null;
        var texto = URLEncoder.encode("Hola, te escribo por mi pedido " + pedido.getFolio(), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + telefonoWhatsapp + "?text=" + texto;
    }

    /** Precio BASE actual del producto; si ya no tiene precios (pedido antiguo), el de la línea. */
    private BigDecimal precioBase(Producto producto, BigDecimal precioLinea) {
        try {
            return productoVentaBO.obtenerPrecioBase(producto);
        } catch (NegocioException e) {
            return precioLinea;
        }
    }

    /** FLY- más el id del pedido con al menos 6 dígitos: FLY-000128. */
    static String formatearFolio(Long pedidoId) {
        return String.format("FLY-%06d", pedidoId);
    }

    private static String rutaComprobante(Long clienteId, String folio) {
        return "pedidos/" + clienteId + "/" + folio + "/";
    }

    private static boolean esPersonal(String rol) {
        return "ADMIN".equalsIgnoreCase(rol) || "EMPLEADO".equalsIgnoreCase(rol);
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static boolean empiezaCon(byte[] datos, int... firma) {
        if (datos.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) if ((datos[i] & 0xFF) != firma[i]) return false;
        return true;
    }

    private static BigDecimal valor(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto;
    }
}
