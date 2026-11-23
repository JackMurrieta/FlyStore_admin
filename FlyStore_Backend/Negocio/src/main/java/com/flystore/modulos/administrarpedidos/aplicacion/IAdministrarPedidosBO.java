package com.flystore.modulos.administrarpedidos.aplicacion;

import com.flystore.contratos.administrarpedidos.BuscarPedidosPeticionDTO;
import com.flystore.contratos.administrarpedidos.DetallePedidoAdminDTO;
import com.flystore.contratos.administrarpedidos.PaginaPedidosDTO;
import com.flystore.contratos.administrarpedidos.ResultadoAccionDTO;
import com.flystore.modulos.administrarpedidos.aplicacion.modelo.DatosEnvio;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * CU-04 Administrar pedidos. El personal (ADMIN o EMPLEADO) consulta los pedidos registrados,
 * valida pagos, avanza el estado y cancela. Los carritos (PENDIENTE) nunca aparecen aquí.
 *
 * <p>{@code adminId} es el identificador de la sesión de quien ejecuta la acción; queda en el
 * historial del pedido. Toda acción responde CONFLICTO_DE_DATOS si el pedido ya no está en el
 * estado que necesita, NO_ENCONTRADO si el folio no existe y DATOS_INVALIDOS si falta un dato.
 */
public interface IAdministrarPedidosBO {

    /** Página de pedidos que cumplen los filtros, del más reciente al más antiguo. */
    PaginaPedidosDTO listarPedidos(BuscarPedidosPeticionDTO peticion);

    /** Pedido con cliente, líneas, comprobante, envío e historial. */
    DetallePedidoAdminDTO obtenerDetalle(String folio);

    /**
     * Cobra en tienda un pedido APARTADO_PENDIENTE_PAGO que no ha vencido: valida el monto, calcula
     * el cambio, descuenta el stock y lo pasa a PAGADO. No avisa al cliente.
     */
    ResultadoAccionDTO registrarPagoEfectivo(String folio, BigDecimal montoRecibido, UUID adminId);

    /** Aprueba el comprobante de un pedido en PAGO_EN_REVISION: descuenta el stock y lo pasa a PAGADO. */
    ResultadoAccionDTO aprobarPago(String folio, UUID adminId);

    /**
     * Rechaza el comprobante de un pedido en PAGO_EN_REVISION: vuelve a PENDIENTE_COMPROBANTE con
     * 42 h nuevas para subir otro. No toca el stock.
     */
    ResultadoAccionDTO rechazarComprobante(String folio, String motivo, String mensaje, UUID adminId);

    /**
     * Avanza el pedido en la secuencia Pagado → Enviado → Entregado. Para ENVIADO exige paquetería y
     * guía, o entrega local.
     *
     * @param estado nombre del estado nuevo.
     */
    ResultadoAccionDTO actualizarEstado(String folio, String estado, DatosEnvio envio, UUID adminId);

    /**
     * Cancela un pedido que no ha salido de la tienda. Si ya estaba pagado repone el stock; la
     * devolución del dinero se gestiona fuera del sistema.
     */
    ResultadoAccionDTO cancelarPedido(String folio, String motivo, UUID adminId);
}
