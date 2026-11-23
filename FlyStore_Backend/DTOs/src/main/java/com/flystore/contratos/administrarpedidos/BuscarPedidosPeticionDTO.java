package com.flystore.contratos.administrarpedidos;

/**
 * CU-04 · Filtros de GET /api/admin/pedidos (query string). Todos son opcionales.
 *
 * @param texto      folio, o nombre o teléfono del cliente.
 * @param estado     nombre de un estado del pedido.
 * @param metodoPago TRANSFERENCIA o EFECTIVO (parámetro {@code metodo_pago}).
 * @param desde      fecha inicial AAAA-MM-DD, incluida.
 * @param hasta      fecha final AAAA-MM-DD, incluida.
 * @param pagina     página pedida, desde 1.
 */
public record BuscarPedidosPeticionDTO(String texto, String estado, String metodoPago, String desde, String hasta, int pagina) {}
