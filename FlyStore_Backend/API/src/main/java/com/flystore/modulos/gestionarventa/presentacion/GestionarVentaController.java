package com.flystore.modulos.gestionarventa.presentacion;

import com.flystore.contratos.gestionarventa.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.IClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.IDatoBancarioBO;
import com.flystore.modulos.gestionarventa.aplicacion.IPedidoVentaBO;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.ArchivoComprobante;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

/**
 * CU-02 Gestionar venta. Funciona como BFF del módulo gestionar-venta del frontend.
 * El cliente nunca envía su id: se obtiene de la sesión (cookie con el JWT de Supabase).
 */
@RestController
@RequestMapping("/api/ventas")
@PreAuthorize("hasRole('CLIENTE')")
public class GestionarVentaController {

    private final IPedidoVentaBO pedidoVentaBO;
    private final IClienteBO clienteBO;
    private final IDatoBancarioBO datoBancarioBO;

    public GestionarVentaController(IPedidoVentaBO pedidoVentaBO, IClienteBO clienteBO, IDatoBancarioBO datoBancarioBO) {
        this.pedidoVentaBO = pedidoVentaBO;
        this.clienteBO = clienteBO;
        this.datoBancarioBO = datoBancarioBO;
    }

    /** Básico · mensaje 4. Carrito actual; vacío si el cliente no ha agregado productos. */
    @GetMapping("/pedido-en-curso")
    public ResponseEntity<PedidoVentaDTO> obtenerPedidoEnCurso(Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.obtenerPedidoEnCurso(idCliente(sesion)));
    }

    /** Agregar al carrito desde el catálogo. */
    @PostMapping("/pedido-en-curso/detalles")
    public ResponseEntity<PedidoVentaDTO> agregarAlCarrito(@Valid @RequestBody AgregarAlCarritoPeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.agregarAlCarrito(idCliente(sesion), peticion));
    }

    /** Alt1 · Cambiar cantidad (0 quita la línea). */
    @PatchMapping("/pedido-en-curso/cantidad")
    public ResponseEntity<PedidoVentaDTO> actualizarCantidad(@Valid @RequestBody ActualizarCantidadPeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.actualizarCantidad(idCliente(sesion), peticion));
    }

    /** Alt2 · Código de descuento. */
    @PostMapping("/pedido-en-curso/codigo-descuento")
    public ResponseEntity<PedidoVentaDTO> aplicarCodigoDescuento(@Valid @RequestBody AplicarCodigoPeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.aplicarCodigoDescuento(idCliente(sesion), peticion));
    }

    /** Básico · mensaje 14. */
    @GetMapping("/datos-entrega")
    public ResponseEntity<DatosEntregaDTO> obtenerDatosEntrega(Authentication sesion) {
        return ResponseEntity.ok(clienteBO.obtenerDatosEntrega(idCliente(sesion)));
    }

    /** Básico · mensaje 29 (datos de la cuenta). */
    @GetMapping("/datos-bancarios")
    public ResponseEntity<DatosBancariosDTO> obtenerDatosBancarios() {
        return ResponseEntity.ok(datoBancarioBO.obtenerDatosBancarios());
    }

    /** Básico · mensaje 29 (folio, concepto de la transferencia). */
    @PostMapping("/pedido-en-curso/folio")
    public ResponseEntity<FolioDTO> generarFolio(Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.generarFolio(idCliente(sesion)));
    }

    /** Básico · mensaje 42; Alt5 responde 400 con el motivo del rechazo. */
    @PostMapping(value = "/pedido-en-curso/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaValidacionComprobanteDTO> subirComprobante(
            @RequestParam("tipoPago") String tipoPago, @RequestParam("folio") String folio,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo, Authentication sesion) throws IOException {
        return ResponseEntity.ok(pedidoVentaBO.subirComprobante(idCliente(sesion), tipoPago, folio, comprobante(archivo)));
    }

    /** S6 · Subir el comprobante de un pedido ya registrado (transferencia «después»). */
    @PostMapping(value = "/pedidos/{folio}/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PedidoVentaDTO> subirComprobantePendiente(@PathVariable String folio,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo, Authentication sesion) throws IOException {
        return ResponseEntity.ok(pedidoVentaBO.subirComprobantePendiente(idCliente(sesion), folio, comprobante(archivo)));
    }

    /** Básico · mensaje 61; Alt3 y Alt4 · mensaje 20. */
    @PostMapping("/pedido-en-curso/registrar")
    public ResponseEntity<PedidoVentaDTO> registrarPedido(@Valid @RequestBody RegistrarPedidoPeticionDTO peticion, Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.registrarPedido(idCliente(sesion), peticion));
    }

    /** Pedido registrado para el ticket que arma el frontend (sustituye al PDF del servidor). */
    @GetMapping("/pedidos/{folio}")
    public ResponseEntity<PedidoVentaDTO> obtenerPedidoRegistrado(@PathVariable String folio, Authentication sesion) {
        return ResponseEntity.ok(pedidoVentaBO.obtenerPedidoRegistrado(idCliente(sesion), folio));
    }

    private static ArchivoComprobante comprobante(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty())
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Selecciona tu comprobante");
        if (archivo.getSize() > ArchivoComprobante.MAX_BYTES)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Sube una imagen JPG o PNG, o un PDF, de máximo 5 MB.");
        return new ArchivoComprobante(archivo.getBytes(), archivo.getContentType(), archivo.getOriginalFilename());
    }

    private Long idCliente(Authentication sesion) {
        return clienteBO.obtenerIdCliente(UUID.fromString(sesion.getName()));
    }
}
