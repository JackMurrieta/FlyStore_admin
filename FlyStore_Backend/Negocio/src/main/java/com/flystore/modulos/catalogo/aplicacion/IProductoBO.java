package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import java.util.List;

public interface IProductoBO {
    void crearProductoConImagen(CrearProductoDTO dto, com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen imagen);

    List<ProductoCatalogoDTO> obtenerProductos();

    List<ProductoCatalogoDTO> buscarProductos(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado);

    PaginaProductosDTO consultarPagina(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado, int pagina, int tamano);

    PaginaProductosDTO buscarPagina(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado, int pagina, int tamano);

    void crearProducto(CrearProductoDTO dto);

    ResultadoBajaDTO darDeBajaProducto(String idProducto);
}
