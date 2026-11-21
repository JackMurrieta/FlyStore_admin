package com.flystore.contratos.gestionarventa;

/**
 * Resultado de subir el comprobante de transferencia.
 *
 * @param comprobanteRuta ruta del archivo en el almacenamiento privado; el front la envía al registrar.
 */
public record RespuestaValidacionComprobanteDTO(String comprobanteRuta, boolean valido, String mensaje) {}
