package com.flystore.modulos.catalogo.aplicacion.modelo;

/** Archivo recibido por la aplicación, sin dependencias de HTTP o del proveedor. */
public record ArchivoImagen(byte[] contenido, String tipoContenido) {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    /** Imágenes nuevas por petición de alta o edición (el servidor acepta hasta 52 MB por petición). */
    public static final int MAX_POR_PETICION = 10;
}
