package com.flystore.modulos.gestionarventa.aplicacion.modelo;

/** Comprobante de pago recibido por la aplicación, sin dependencias de HTTP ni del proveedor. */
public record ArchivoComprobante(byte[] contenido, String tipoContenido, String nombre) {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
}
