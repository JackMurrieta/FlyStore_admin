package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.contratos.gestionarventa.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.IClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.TotalesPedido;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.IAlmacenComprobantes;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IPedidoVentaDAO;
import com.flystore.persistencia.entidades.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Coordina CU-02 Gestionar venta. Las reglas de líneas, precios, existencia y códigos están en sus
 * propios BO; este BO decide el flujo, el estado del pedido y las transacciones.
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

    private static final Pattern FOLIO = Pattern.compile("FLY-\\d{6,}");
    private static final Map<String, String> EXTENSIONES = Map.of("image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");
    private static final String COMPROBANTE_INVALIDO = "Sube una imagen JPG o PNG, o un PDF, de máximo 5 MB.";

    private final IPedidoVentaDAO pedidoVentaDAO;
    private final IClienteDAO clienteDAO;
    private final IDetallePedidoBO detallePedidoBO;
    private final IProductoVentaBO productoVentaBO;
    private final IVarianteProductoBO varianteProductoBO;
    private final ICodigoDescuentoBO codigoDescuentoBO;
    private final IClienteBO clienteBO;
    private final IAlmacenComprobantes almacenComprobantes;
    private final PedidoVentaMapper mapper;
    private final Clock reloj;
    private final String telefonoWhatsapp;

    public PedidoVentaBO(IPedidoVentaDAO pedidoVentaDAO, IClienteDAO clienteDAO, IDetallePedidoBO detallePedidoBO,
                         IProductoVentaBO productoVentaBO, IVarianteProductoBO varianteProductoBO,
                         ICodigoDescuentoBO codigoDescuentoBO, IClienteBO clienteBO,
                         IAlmacenComprobantes almacenComprobantes, PedidoVentaMapper mapper, Clock reloj,
                         @Value("${app.ventas.whatsapp:}") String telefonoWhatsapp) {
        this.pedidoVentaDAO = pedidoVentaDAO;
        this.clienteDAO = clienteDAO;
        this.detallePedidoBO = detallePedidoBO;
        this.productoVentaBO = productoVentaBO;
        this.varianteProductoBO = varianteProductoBO;
        this.codigoDescuentoBO = codigoDescuentoBO;
        this.clienteBO = clienteBO;
        this.almacenComprobantes = almacenComprobantes;
        this.mapper = mapper;
        this.reloj = reloj;
        this.telefonoWhatsapp = telefonoWhatsapp == null ? "" : telefonoWhatsapp.replaceAll("\\D", "");
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

    // ===== Confirmación de pagos (personal) =====

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
    public PedidoVentaDTO confirmarPago(String idPedido, ConfirmarPagoPeticionDTO peticion) {
        var pedido = pedidoVentaDAO.bloquearPorId(PedidoVentaMapper.id(idPedido))
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "No se encontró el pedido con ID: " + idPedido));
        if (!ESTADOS_POR_CONFIRMAR.contains(pedido.getEstadoVenta()) || pedido.getPago() == null)
            throw new NegocioException(TipoError.CONFLICTO_DE_DATOS, "El pedido " + pedido.getFolio() + " no está pendiente de confirmar su pago");
        var pago = pedido.getPago().sinProxy();
        if (pago instanceof PagoEfectivo efectivo) {
            var recibido = peticion == null ? null : peticion.cantidadRecibida();
            if (recibido == null)
                throw new NegocioException(TipoError.DATOS_INVALIDOS, "Captura la cantidad recibida en efectivo");
            if (recibido.compareTo(valor(pago.getMonto())) < 0)
                throw new NegocioException(TipoError.DATOS_INVALIDOS, "La cantidad recibida es menor al total del pedido ($" + valor(pago.getMonto()) + ")");
            efectivo.setCantidadRecibida(recibido);
            efectivo.setCambio(recibido.subtract(valor(pago.getMonto())));
        }
        // Primero el stock: si no alcanza, se lanza CONFLICTO_DE_DATOS y el pedido conserva su estado.
        varianteProductoBO.descontarExistencia(pedido);
        pago.setEstadoPago(EstadoPago.COMPLETADO);
        pago.setFechaPago(LocalDate.now(reloj));
        pedido.setEstadoVenta(EstadoVenta.PAGADO);
        pedidoVentaDAO.guardarPedidoVenta(pedido);
        logger.info("Pago del pedido {} confirmado", pedido.getFolio());
        return aPedidoVentaDTO(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteUrlDTO obtenerUrlComprobante(String idPedido) {
        var pedido = pedidoVentaDAO.obtenerPorIdCompleto(PedidoVentaMapper.id(idPedido))
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "No se encontró el pedido con ID: " + idPedido));
        if (pedido.getPago() == null || !(pedido.getPago().sinProxy() instanceof PagoTransferencia transferencia)
                || !tieneTexto(transferencia.getComprobantePagoUrl()))
            throw new NegocioException(TipoError.NO_ENCONTRADO, "El pedido no tiene comprobante");
        return new ComprobanteUrlDTO(almacenComprobantes.generarUrlTemporal(transferencia.getComprobantePagoUrl(), VIGENCIA_URL_COMPROBANTE),
                VIGENCIA_URL_COMPROBANTE);
    }

    @Override
    @Transactional
    public int cancelarPedidosVencidos() {
        var limite = OffsetDateTime.now(reloj).minus(PLAZO_PAGO);
        var vencidos = pedidoVentaDAO.listarPorEstadosRegistradosAntesDe(ESTADOS_SIN_PAGO, limite);
        for (var pedido : vencidos) {
            codigoDescuentoBO.liberarUso(detallePedidoBO.obtenerCodigosAplicados(pedido));
            pedido.setEstadoVenta(EstadoVenta.CANCELADO_VENCIMIENTO);
            logger.info("Pedido {} cancelado por vencimiento", pedido.getFolio());
        }
        return vencidos.size();
    }

    // ===== Reglas propias del pedido =====

    /** Estado con el que nace el pedido según la forma de pago. */
    EstadoVenta determinarEstadoInicial(MetodoPagoDTO metodoPago) {
        if ("EFECTIVO".equals(metodoPago.tipoPago())) return EstadoVenta.APARTADO_PENDIENTE_PAGO;
        return tieneTexto(metodoPago.comprobanteRuta()) ? EstadoVenta.PAGO_EN_REVISION : EstadoVenta.PENDIENTE_COMPROBANTE;
    }

    /** Vencimiento de un pedido registrado sin pago; null en los demás estados. */
    OffsetDateTime calcularFechaLimite(PedidoVenta pedido) {
        return ESTADOS_SIN_PAGO.contains(pedido.getEstadoVenta()) && pedido.getFechaHoraPedido() != null
                ? pedido.getFechaHoraPedido().plus(PLAZO_PAGO) : null;
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
