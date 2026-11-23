package com.flystore.modulos.catalogo.aplicacion.puertos;

import com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen;
import com.flystore.modulos.catalogo.aplicacion.modelo.ImagenAlmacenada;
import java.util.Optional;

public interface IAlmacenImagenes {
    ImagenAlmacenada subir(ArchivoImagen archivo);
    void eliminar(String ruta);

    /**
     * Ruta en Storage de una URL pública que subió el propio backend; vacío si la URL es externa o
     * de otra carpeta (esas imágenes nunca se borran desde aquí).
     */
    Optional<String> rutaDe(String url);
}
