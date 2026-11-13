package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import java.util.List;

public interface IProductoBO {

    List<ProductoCatalogoDTO> obtenerProductos();

    List<ProductoCatalogoDTO> buscarProductos(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado);

    void crearProducto(CrearProductoDTO dto);

    ResultadoBajaDTO darDeBajaProducto(String idProducto);
}
