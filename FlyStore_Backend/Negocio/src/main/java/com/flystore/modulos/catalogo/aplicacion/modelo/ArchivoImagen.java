package com.flystore.modulos.catalogo.aplicacion.modelo;

/** Archivo recibido por la aplicación, sin dependencias de HTTP o del proveedor. */
public record ArchivoImagen(byte[] contenido, String tipoContenido) {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
}
