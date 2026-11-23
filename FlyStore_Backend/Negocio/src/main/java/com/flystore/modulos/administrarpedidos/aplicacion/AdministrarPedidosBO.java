package com.flystore.modulos.administrarpedidos.aplicacion;

import com.flystore.contratos.administrarpedidos.BuscarPedidosPeticionDTO;
import com.flystore.contratos.administrarpedidos.DetallePedidoAdminDTO;
import com.flystore.contratos.administrarpedidos.PaginaPedidosDTO;
import com.flystore.contratos.administrarpedidos.ResultadoAccionDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.administrarpedidos.aplicacion.mappers.PedidoAdminBOMapper;
import com.flystore.modulos.administrarpedidos.aplicacion.modelo.DatosEnvio;
import com.flystore.modulos.administrarpedidos.aplicacion.modelo.PedidoAdminCompleto;
import com.flystore.modulos.administrarpedidos.aplicacion.modelo.ResultadoAccion;
import com.flystore.modulos.administrarpedidos.aplicacion.puertos.INotificadorCliente;
import com.flystore.modulos.administrarpedidos.aplicacion.reglas.ConstructorMensajeCliente;
import com.flystore.modulos.administrarpedidos.aplicacion.reglas.PoliticaEstadosPedido;
import com.flystore.modulos.administrarpedidos.aplicacion.reglas.PoliticaPlazos;
import com.flystore.modulos.administrarpedidos.aplicacion.reglas.ValidadorPagoEfectivo;
import com.flystore.modulos.gestionarventa.aplicacion.ICodigoDescuentoBO;
import com.flystore.modulos.gestionarventa.aplicacion.IDetallePedidoBO;
import com.flystore.modulos.gestionarventa.aplicacion.IVarianteProductoBO;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.IAlmacenComprobantes;
import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.dao.interfaces.IHistorialPedidoDAO;
import com.flystore.persistencia.dao.interfaces.IPedidoVentaDAO;
import com.flystore.persistencia.entidades.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Coordina CU-04 Administrar pedidos. Las reglas de estados, plazos, cobro en efectivo y textos
 * están en sus propias clases; la existencia y los códigos de descuento se piden a los BO de CU-02.
 *
 * <p>Cada acción cambia el pedido dentro de una transacción y, ya confirmada, avisa al cliente:
 * si el aviso falla, el cambio no se revierte y la respuesta lleva notificacion_enviada = false.
 */
@Service
public class AdministrarPedidosBO implements IAdministrarPedidosBO {

    private static final Logger logger = LoggerFactory.getLogger(AdministrarPedidosBO.class);

    /** Segundos que dura el enlace para ver un comprobante. */
    static final int VIGENCIA_URL_COMPROBANTE = 300;
    static final String MOTIVO_PAGO_EFECTIVO = "pago en efectivo";
    static final String USUARIO_DESCONOCIDO = "Personal de FlyStore";

    private static final Pattern FOLIO = Pattern.compile("FLY-\\d{6,}");
    /** Un texto que empieza como folio se busca solo en el folio, sin consultar clientes. */
    private static final Pattern PARECE_FOLIO = Pattern.compile("(?i)fly[-\\d]*");
    /** Estados en los que el stock del pedido ya se descontó. */
    private static final Set<EstadoVenta> CON_STOCK_DESCONTADO = EnumSet.of(EstadoVenta.PAGADO, EstadoVenta.PREPARACION);

    private final IPedidoVentaDAO pedidoVentaDAO;
    private final IClienteDAO clienteDAO;
    private final IHistorialPedidoDAO historialPedidoDAO;
    private final IAdminDAO adminDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IVarianteProductoBO varianteProductoBO;
    private final ICodigoDescuentoBO codigoDescuentoBO;
    private final IDetallePedidoBO detallePedidoBO;
    private final IAlmacenComprobantes almacenComprobantes;
    private final INotificadorCliente notificador;
    private final PoliticaEstadosPedido politicaEstados;
    private final PoliticaPlazos politicaPlazos;
    private final ValidadorPagoEfectivo validadorPagoEfectivo;
    private final ConstructorMensajeCliente mensajes;
    private final PedidoAdminBOMapper mapper;
    private final Clock reloj;
    private final TransactionOperations transacciones;

    @Autowired
    public AdministrarPedidosBO(IPedidoVentaDAO pedidoVentaDAO, IClienteDAO clienteDAO, IHistorialPedidoDAO historialPedidoDAO,
                                IAdminDAO adminDAO, IEmpleadoDAO empleadoDAO, IVarianteProductoBO varianteProductoBO,
                                ICodigoDescuentoBO codigoDescuentoBO, IDetallePedidoBO detallePedidoBO,
                                IAlmacenComprobantes almacenComprobantes, INotificadorCliente notificador,
                                PoliticaEstadosPedido politicaEstados, PoliticaPlazos politicaPlazos,
                                ValidadorPagoEfectivo validadorPagoEfectivo, ConstructorMensajeCliente mensajes,
                                PedidoAdminBOMapper mapper, Clock reloj, PlatformTransactionManager gestorTransacciones) {
        this(pedidoVentaDAO, clienteDAO, historialPedidoDAO, adminDAO, empleadoDAO, varianteProductoBO, codigoDescuentoBO,
                detallePedidoBO, almacenComprobantes, notificador, politicaEstados, politicaPlazos, validadorPagoEfectivo,
                mensajes, mapper, reloj, new TransactionTemplate(gestorTransacciones));
    }

    /** Para pruebas: recibe directamente quién abre y cierra las transacciones. */
    AdministrarPedidosBO(IPedidoVentaDAO pedidoVentaDAO, IClienteDAO clienteDAO, IHistorialPedidoDAO historialPedidoDAO,
                         IAdminDAO adminDAO, IEmpleadoDAO empleadoDAO, IVarianteProductoBO varianteProductoBO,
                         ICodigoDescuentoBO codigoDescuentoBO, IDetallePedidoBO detallePedidoBO,
                         IAlmacenComprobantes almacenComprobantes, INotificadorCliente notificador,
                         PoliticaEstadosPedido politicaEstados, PoliticaPlazos politicaPlazos,
                         ValidadorPagoEfectivo validadorPagoEfectivo, ConstructorMensajeCliente mensajes,
                         PedidoAdminBOMapper mapper, Clock reloj, TransactionOperations transacciones) {
        this.pedidoVentaDAO = pedidoVentaDAO;
        this.clienteDAO = clienteDAO;
        this.historialPedidoDAO = historialPedidoDAO;
        this.adminDAO = adminDAO;
        this.empleadoDAO = empleadoDAO;
        this.varianteProductoBO = varianteProductoBO;
        this.codigoDescuentoBO = codigoDescuentoBO;
        this.detallePedidoBO = detallePedidoBO;
        this.almacenComprobantes = almacenComprobantes;
        this.notificador = notificador;
        this.politicaEstados = politicaEstados;
        this.politicaPlazos = politicaPlazos;
        this.validadorPagoEfectivo = validadorPagoEfectivo;
        this.mensajes = mensajes;
        this.mapper = mapper;
        this.reloj = reloj;
        this.transacciones = transacciones;
    }

    // ===== Consultas =====

    @Override
    @Transactional(readOnly = true)
    public PaginaPedidosDTO listarPedidos(BuscarPedidosPeticionDTO peticion) {
        var criterios = mapper.toCriterios(peticion);
        // Si el texto no es un folio, puede ser el nombre o el teléfono de un cliente.
        List<Long> clienteIds = criterios.texto() == null || PARECE_FOLIO.matcher(criterios.texto()).matches() ? null
                : clienteDAO.buscarPorNombreOTelefono(criterios.texto()).stream().map(Cliente::getId).toList();
        return mapper.toPaginaDTO(pedidoVentaDAO.buscarPorCriterios(criterios, clienteIds));
    }

    @Override
    @Transactional(readOnly = true)
    public DetallePedidoAdminDTO obtenerDetalle(String folio) {
        var pedido = pedidoVentaDAO.obtenerPorFolioCompleto(folioValido(folio))
                .filter(p -> p.getEstadoVenta() != EstadoVenta.PENDIENTE)
                .orElseThrow(AdministrarPedidosBO::noEncontrado);
        return mapper.toDetalleDTO(cargarCompleto(pedido));
    }

    // ===== Acciones =====

    @Override
    public ResultadoAccionDTO registrarPagoEfectivo(String folio, BigDecimal montoRecibido, UUID adminId) {
        return ejecutar(() -> {
            var pedido = bloquear(folio);
            var ahora = OffsetDateTime.now(reloj);
            // Vencido aunque la tarea de vencimiento aún no lo haya cancelado: ya no se cobra.
            if (pedido.getEstadoVenta() != EstadoVenta.APARTADO_PENDIENTE_PAGO || politicaPlazos.estaVencido(pedido, ahora)
                    || !(pago(pedido) instanceof PagoEfectivo efectivo))
                throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                        "El apartado venció o el pedido cambió de estado. El cliente debe hacer un nuevo pedido.");
            var cambio = validadorPagoEfectivo.validar(valor(efectivo.getMonto()), montoRecibido);
            descontarPiezas(pedido);
            efectivo.setCantidadRecibida(montoRecibido);
            efectivo.setCambio(cambio);
            efectivo.setEstadoPago(EstadoPago.COMPLETADO);
            efectivo.setFechaPago(LocalDate.now(reloj));
            cambiarEstado(pedido, EstadoVenta.PAGADO, nombreUsuario(adminId), MOTIVO_PAGO_EFECTIVO, false);
            // El cliente pagó en la tienda: no hace falta avisarle.
            return resultado(pedido, null);
        });
    }

    @Override
    public ResultadoAccionDTO aprobarPago(String folio, UUID adminId) {
        return ejecutar(() -> {
            var pedido = bloquear(folio);
            politicaEstados.exigirEstado(pedido, EstadoVenta.PAGO_EN_REVISION);
            var transferencia = pagoTransferencia(pedido);
            // Primero el stock: si no alcanza se lanza CONFLICTO_DE_DATOS y el pedido conserva su estado.
            descontarPiezas(pedido);
            transferencia.setEstadoPago(EstadoPago.COMPLETADO);
            transferencia.setFechaPago(LocalDate.now(reloj));
            cambiarEstado(pedido, EstadoVenta.PAGADO, nombreUsuario(adminId), null, false);
            return resultado(pedido, mensajes.pagoValidado(pedido));
        });
    }

    @Override
    public ResultadoAccionDTO rechazarComprobante(String folio, String motivo, String mensaje, UUID adminId) {
        var motivoLimpio = motivoObligatorio(motivo, "Indica el motivo del rechazo.");
        return ejecutar(() -> {
            var pedido = bloquear(folio);
            politicaEstados.exigirEstado(pedido, EstadoVenta.PAGO_EN_REVISION);
            var transferencia = pagoTransferencia(pedido);
            // El archivo rechazado se conserva hasta que el cliente suba otro; el stock no se toca.
            transferencia.setEstadoPago(EstadoPago.PENDIENTE);
            var nuevoLimite = politicaPlazos.nuevoLimiteComprobante(OffsetDateTime.now(reloj));
            var anterior = pedido.getEstadoVenta();
            pedido.cambiarEstado(EstadoVenta.PENDIENTE_COMPROBANTE, nuevoLimite);
            pedidoVentaDAO.guardarPedidoVenta(pedido);
            registrarHistorial(pedido, anterior, nombreUsuario(adminId), motivoLimpio, true);
            return resultado(pedido, mensajes.comprobanteRechazado(pedido, motivoLimpio, mensaje, nuevoLimite));
        });
    }

    @Override
    public ResultadoAccionDTO actualizarEstado(String folio, String estado, DatosEnvio envio, UUID adminId) {
        var nuevo = estadoValido(estado);
        return ejecutar(() -> {
            var pedido = bloquear(folio);
            politicaEstados.validarTransicion(pedido.getEstadoVenta(), nuevo);
            politicaEstados.requiereDatosEnvio(nuevo, envio);
            if (nuevo == EstadoVenta.ENVIADO) pedido.registrarEnvio(envio.paqueteria(), envio.guia(), envio.entregaLocal());
            cambiarEstado(pedido, nuevo, nombreUsuario(adminId), null, false);
            return resultado(pedido, mensajes.cambioEstado(pedido, pedido.getGuia()));
        });
    }

    @Override
    public ResultadoAccionDTO cancelarPedido(String folio, String motivo, UUID adminId) {
        var motivoLimpio = motivoObligatorio(motivo, "Indica el motivo de la cancelación.");
        return ejecutar(() -> {
            var pedido = bloquear(folio);
            var estado = pedido.getEstadoVenta();
            if (!politicaEstados.puedeCancelarse(estado))
                throw new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                        estado == EstadoVenta.CANCELADO || estado == EstadoVenta.CANCELADO_VENCIMIENTO
                                ? "El pedido ya está cancelado."
                                : "El pedido ya salió de la tienda y no se puede cancelar.");
            // Solo un pedido ya pagado tiene piezas descontadas que devolver al inventario.
            if (CON_STOCK_DESCONTADO.contains(estado)) liberarPiezas(pedido);
            // Igual que al vencer: el código de descuento vuelve a estar disponible.
            codigoDescuentoBO.liberarUso(detallePedidoBO.obtenerCodigosAplicados(pedido));
            cambiarEstado(pedido, EstadoVenta.CANCELADO, nombreUsuario(adminId), motivoLimpio, false);
            return resultado(pedido, mensajes.cancelacion(pedido, motivoLimpio));
        });
    }

    // ===== Pasos compartidos =====

    /** Ejecuta el cambio en una transacción y, ya confirmada, avisa al cliente. */
    private ResultadoAccionDTO ejecutar(Supplier<ResultadoAccion> accion) {
        var resultado = transacciones.execute(estado -> accion.get());
        return mapper.toResultadoDTO(resultado, notificar(resultado));
    }

    /** Pedido registrado con bloqueo de escritura: dos personas no lo cambian a la vez. */
    private PedidoVenta bloquear(String folio) {
        return pedidoVentaDAO.bloquearPorFolio(folioValido(folio))
                .filter(p -> p.getEstadoVenta() != EstadoVenta.PENDIENTE)
                .orElseThrow(AdministrarPedidosBO::noEncontrado);
    }

    /** Cambia el estado, guarda el pedido y deja la entrada en el historial. */
    private void cambiarEstado(PedidoVenta pedido, EstadoVenta nuevo, String usuario, String motivo, boolean comprobanteRechazado) {
        var anterior = pedido.getEstadoVenta();
        pedido.cambiarEstado(nuevo);
        pedidoVentaDAO.guardarPedidoVenta(pedido);
        registrarHistorial(pedido, anterior, usuario, motivo, comprobanteRechazado);
    }

    void descontarPiezas(PedidoVenta pedido) {
        varianteProductoBO.descontarExistencia(pedido);
    }

    void liberarPiezas(PedidoVenta pedido) {
        varianteProductoBO.reponerExistencia(pedido);
    }

    void registrarHistorial(PedidoVenta pedido, EstadoVenta anterior, String usuario, String motivo, boolean comprobanteRechazado) {
        var entrada = new HistorialPedido();
        entrada.setPedidoVenta(pedido);
        entrada.setFechaHora(OffsetDateTime.now(reloj));
        entrada.setUsuario(usuario);
        entrada.setEstadoAnterior(anterior);
        entrada.setEstadoNuevo(pedido.getEstadoVenta());
        entrada.setMotivo(motivo);
        entrada.setComprobanteRechazado(comprobanteRechazado);
        historialPedidoDAO.guardar(entrada);
        logger.info("Pedido {}: {} -> {} por {}", pedido.getFolio(), anterior, pedido.getEstadoVenta(), usuario);
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

    private ResultadoAccion resultado(PedidoVenta pedido, String mensaje) {
        return new ResultadoAccion(mapper.toDetalleDTO(cargarCompleto(pedido)), mapper.telefonoCliente(pedido), mensaje);
    }

    private PedidoAdminCompleto cargarCompleto(PedidoVenta pedido) {
        return new PedidoAdminCompleto(pedido, historialPedidoDAO.listarPorPedido(pedido.getId()), urlComprobante(pedido));
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

    /** Nombre de quien hace el cambio, para el historial. */
    private String nombreUsuario(UUID adminId) {
        if (adminId == null) return USUARIO_DESCONOCIDO;
        Usuario usuario = adminDAO.findByAuthId(adminId).map(Usuario.class::cast)
                .or(() -> empleadoDAO.findByAuthId(adminId).map(Usuario.class::cast)).orElse(null);
        if (usuario == null) return USUARIO_DESCONOCIDO;
        var nombre = String.join(" ", Stream.of(usuario.getNombre(), usuario.getApellidos())
                .filter(AdministrarPedidosBO::tieneTexto).map(String::trim).toList());
        return nombre.isEmpty() ? USUARIO_DESCONOCIDO : nombre;
    }

    private PagoTransferencia pagoTransferencia(PedidoVenta pedido) {
        if (pago(pedido) instanceof PagoTransferencia transferencia) return transferencia;
        throw new NegocioException(TipoError.CONFLICTO_DE_DATOS, "El pedido " + pedido.getFolio() + " no se pagó por transferencia.");
    }

    /** Pago real del pedido (sin el proxy de Hibernate); null si no tiene. */
    private static Pago pago(PedidoVenta pedido) {
        return pedido.getPago() == null ? null : pedido.getPago().sinProxy();
    }

    private static String folioValido(String folio) {
        var limpio = folio == null ? "" : folio.trim().toUpperCase(Locale.ROOT);
        if (!FOLIO.matcher(limpio).matches()) throw noEncontrado();
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

    private static NegocioException noEncontrado() {
        return new NegocioException(TipoError.NO_ENCONTRADO, "No existe un pedido con ese folio.");
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static BigDecimal valor(BigDecimal monto) {
        return monto == null ? BigDecimal.ZERO : monto;
    }
}
