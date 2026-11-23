package com.flystore.modulos.administrarpedidos.presentacion;

import com.flystore.contratos.administrarpedidos.ActualizarEstadoPeticionDTO;
import com.flystore.contratos.administrarpedidos.BuscarPedidosPeticionDTO;
import com.flystore.contratos.administrarpedidos.CancelarPedidoPeticionDTO;
import com.flystore.contratos.administrarpedidos.DetallePedidoAdminDTO;
import com.flystore.contratos.administrarpedidos.PaginaPedidosDTO;
import com.flystore.contratos.administrarpedidos.RechazarComprobantePeticionDTO;
import com.flystore.contratos.administrarpedidos.RegistrarPagoEfectivoPeticionDTO;
import com.flystore.contratos.administrarpedidos.ResultadoAccionDTO;
import com.flystore.modulos.administrarpedidos.aplicacion.IAdministrarPedidosBO;
import com.flystore.modulos.administrarpedidos.aplicacion.modelo.DatosEnvio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CU-04 Administrar pedidos. Solo ADMIN y EMPLEADO; quién ejecuta cada acción sale de la sesión
 * (cookie), nunca del cuerpo de la petición. El JSON usa snake_case, como lo espera el frontend.
 * Los errores los traduce GlobalExceptionHandler: 404 folio inexistente, 409 el pedido ya no está
 * en el estado que la acción necesita, 400 falta un dato o es inválido.
 */
@RestController
@RequestMapping("/api/admin/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
public class AdministrarPedidosController {

    private final IAdministrarPedidosBO pedidosBO;

    public AdministrarPedidosController(IAdministrarPedidosBO pedidosBO) {
        this.pedidosBO = pedidosBO;
    }

    /** Lista con filtros opcionales: texto (folio, nombre o teléfono), estado, método de pago y rango de fechas. */
    @GetMapping
    public ResponseEntity<PaginaPedidosDTO> listarPedidos(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estado,
            @RequestParam(name = "metodo_pago", required = false) String metodoPago,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(defaultValue = "1") int pagina) {
        return ResponseEntity.ok(pedidosBO.listarPedidos(new BuscarPedidosPeticionDTO(texto, estado, metodoPago, desde, hasta, pagina)));
    }

    @GetMapping("/{folio}")
    public ResponseEntity<DetallePedidoAdminDTO> obtenerDetalle(@PathVariable String folio) {
        return ResponseEntity.ok(pedidosBO.obtenerDetalle(folio));
    }

    /** Cobro en tienda de un pedido apartado: descuenta el stock y lo pasa a PAGADO. */
    @PostMapping("/{folio}/pago-efectivo")
    public ResponseEntity<ResultadoAccionDTO> registrarPagoEfectivo(@PathVariable String folio,
            @Valid @RequestBody RegistrarPagoEfectivoPeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidosBO.registrarPagoEfectivo(folio, peticion.montoRecibido(), adminId(sesion)));
    }

    /** Comprobante correcto: descuenta el stock y pasa el pedido a PAGADO. */
    @PostMapping("/{folio}/aprobar-pago")
    public ResponseEntity<ResultadoAccionDTO> aprobarPago(@PathVariable String folio, Authentication sesion) {
        return ResponseEntity.ok(pedidosBO.aprobarPago(folio, adminId(sesion)));
    }

    /** Comprobante incorrecto: el pedido vuelve a PENDIENTE_COMPROBANTE con 42 h nuevas. */
    @PostMapping("/{folio}/rechazar-comprobante")
    public ResponseEntity<ResultadoAccionDTO> rechazarComprobante(@PathVariable String folio,
            @Valid @RequestBody RechazarComprobantePeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidosBO.rechazarComprobante(folio, peticion.motivo(), peticion.mensaje(), adminId(sesion)));
    }

    /** Avanza el pedido: Pagado → Enviado → Entregado. */
    @PatchMapping("/{folio}/estado")
    public ResponseEntity<ResultadoAccionDTO> actualizarEstado(@PathVariable String folio,
            @Valid @RequestBody ActualizarEstadoPeticionDTO peticion, Authentication sesion) {
        var envio = new DatosEnvio(peticion.paqueteria(), peticion.guia(), peticion.entregaLocal());
        return ResponseEntity.ok(pedidosBO.actualizarEstado(folio, peticion.estado(), envio, adminId(sesion)));
    }

    /** Cancela un pedido que no ha salido de la tienda; si ya estaba pagado, repone el stock. */
    @PostMapping("/{folio}/cancelar")
    public ResponseEntity<ResultadoAccionDTO> cancelarPedido(@PathVariable String folio,
            @Valid @RequestBody CancelarPedidoPeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidosBO.cancelarPedido(folio, peticion.motivo(), adminId(sesion)));
    }

    /** Identificador de la sesión de quien ejecuta la acción (el «sub» del JWT). */
    private static UUID adminId(Authentication sesion) {
        try {
            return UUID.fromString(sesion.getName());
        } catch (RuntimeException e) {
            // Sin un identificador válido el cambio se registra a nombre del personal en general.
            return null;
        }
    }
}
