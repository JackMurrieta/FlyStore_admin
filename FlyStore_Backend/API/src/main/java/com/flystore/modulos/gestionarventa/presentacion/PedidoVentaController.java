package com.flystore.modulos.gestionarventa.presentacion;

import com.flystore.contratos.gestionarventa.ComprobanteUrlDTO;
import com.flystore.contratos.gestionarventa.ConfirmarPagoPeticionDTO;
import com.flystore.contratos.gestionarventa.PedidoVentaDTO;
import jakarta.validation.Valid;
import com.flystore.modulos.autenticacion.aplicacion.IClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.IPedidoVentaBO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Pedidos registrados. ADMIN y EMPLEADO ven todos y confirman pagos; CLIENTE solo ve los suyos.
 * Los carritos (pedidos PENDIENTE) no aparecen aquí.
 */
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO', 'CLIENTE')")
public class PedidoVentaController {

    private final IPedidoVentaBO pedidoVentaBO;
    private final IClienteBO clienteBO;

    public PedidoVentaController(IPedidoVentaBO pedidoVentaBO, IClienteBO clienteBO) {
        this.pedidoVentaBO = pedidoVentaBO;
        this.clienteBO = clienteBO;
    }

    @GetMapping
    public ResponseEntity<List<PedidoVentaDTO>> listarPedidos(Authentication sesion) {
        var rol = rol(sesion);
        return ResponseEntity.ok(pedidoVentaBO.listarPedidos(rol, idCliente(sesion, rol)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoVentaDTO> obtenerPedido(@PathVariable String id, Authentication sesion) {
        var rol = rol(sesion);
        return ResponseEntity.ok(pedidoVentaBO.obtenerPedidoCompleto(id, rol, idCliente(sesion, rol)));
    }

    /** Pedidos que esperan confirmación de pago, del más antiguo al más reciente (FIFO). */
    @GetMapping("/por-confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<List<PedidoVentaDTO>> listarPorConfirmar() {
        return ResponseEntity.ok(pedidoVentaBO.listarPorConfirmar());
    }

    /** Comprobante revisado o efectivo recibido: descuenta el stock y pasa el pedido a PAGADO. */
    @PostMapping("/{id}/confirmar-pago")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<PedidoVentaDTO> confirmarPago(@PathVariable String id,
            @Valid @RequestBody(required = false) ConfirmarPagoPeticionDTO peticion) {
        return ResponseEntity.ok(pedidoVentaBO.confirmarPago(id, peticion));
    }

    /** Enlace temporal (5 min) al comprobante de transferencia del pedido. */
    @GetMapping("/{id}/comprobante")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    public ResponseEntity<ComprobanteUrlDTO> obtenerComprobante(@PathVariable String id) {
        return ResponseEntity.ok(pedidoVentaBO.obtenerUrlComprobante(id));
    }

    /** El personal no necesita id de cliente; un cliente siempre se resuelve desde la sesión. */
    private Long idCliente(Authentication sesion, String rol) {
        return "CLIENTE".equals(rol) ? clienteBO.obtenerIdCliente(UUID.fromString(sesion.getName())) : null;
    }

    private static String rol(Authentication sesion) {
        var roles = sesion.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        if (roles.contains("ROLE_ADMIN")) return "ADMIN";
        if (roles.contains("ROLE_EMPLEADO")) return "EMPLEADO";
        return "CLIENTE";
    }
}
