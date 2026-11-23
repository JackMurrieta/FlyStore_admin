package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.modulos.autenticacion.aplicacion.IAdminBO;
import com.flystore.modulos.autenticacion.aplicacion.IEmpleadoBO;
import com.flystore.persistencia.dao.interfaces.IHistorialPedidoDAO;
import com.flystore.persistencia.entidades.EstadoVenta;
import com.flystore.persistencia.entidades.HistorialPedido;
import com.flystore.persistencia.entidades.PedidoVenta;
import com.flystore.persistencia.entidades.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Guarda y consulta el historial de cambios de estado de los pedidos. Usa solo IHistorialPedidoDAO. */
@Service
public class HistorialPedidoBO implements IHistorialPedidoBO {

    private static final Logger logger = LoggerFactory.getLogger(HistorialPedidoBO.class);

    private final IHistorialPedidoDAO historialPedidoDAO;
    private final IAdminBO adminBO;
    private final IEmpleadoBO empleadoBO;
    private final Clock reloj;

    public HistorialPedidoBO(IHistorialPedidoDAO historialPedidoDAO, IAdminBO adminBO, IEmpleadoBO empleadoBO, Clock reloj) {
        this.historialPedidoDAO = historialPedidoDAO;
        this.adminBO = adminBO;
        this.empleadoBO = empleadoBO;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void registrarCambio(PedidoVenta pedido, EstadoVenta anterior, UUID usuarioAuthId, String motivo, boolean comprobanteRechazado) {
        var usuario = nombreUsuario(usuarioAuthId);
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

    @Override
    @Transactional(readOnly = true)
    public List<HistorialPedido> listarPorPedido(Long pedidoId) {
        return historialPedidoDAO.listarPorPedido(pedidoId);
    }

    /** Nombre y apellidos del administrador o empleado de la sesión. */
    String nombreUsuario(UUID authId) {
        if (authId == null) return USUARIO_DESCONOCIDO;
        Usuario usuario = adminBO.findByAuthId(authId).map(Usuario.class::cast)
                .or(() -> empleadoBO.findByAuthId(authId).map(Usuario.class::cast)).orElse(null);
        if (usuario == null) return USUARIO_DESCONOCIDO;
        var nombre = String.join(" ", Stream.of(usuario.getNombre(), usuario.getApellidos())
                .filter(valor -> valor != null && !valor.isBlank()).map(String::trim).toList());
        return nombre.isEmpty() ? USUARIO_DESCONOCIDO : nombre;
    }
}
