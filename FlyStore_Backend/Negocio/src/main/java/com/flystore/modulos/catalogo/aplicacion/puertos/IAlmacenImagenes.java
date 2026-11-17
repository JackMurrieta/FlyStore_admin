package com.flystore.modulos.catalogo.aplicacion.puertos;

import com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen;
import com.flystore.modulos.catalogo.aplicacion.modelo.ImagenAlmacenada;

public interface IAlmacenImagenes {
    ImagenAlmacenada subir(ArchivoImagen archivo);
    void eliminar(String ruta);
}
