package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen;
import java.util.List;

public interface IProductoBO {
    /**
     * Alta con archivos: {@code imagenes} del DTO dice el color de cada archivo (por su posición en
     * {@code archivos}) y cada color de las variantes debe tener al menos una imagen.
     */
    void crearProductoConImagenes(CrearProductoDTO dto, List<ArchivoImagen> archivos);

    List<ProductoCatalogoDTO> obtenerProductos();

    List<ProductoCatalogoDTO> buscarProductos(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado);

    PaginaProductosDTO consultarPagina(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado, int pagina, int tamano);

    PaginaProductosDTO buscarPagina(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado, int pagina, int tamano);

    /** Alta por JSON: imágenes por URL, cada una con su color. */
    void crearProducto(CrearProductoDTO dto);

    /** CU-03 S1 · Producto completo para el formulario de edición. */
    ProductoEdicionDTO obtenerProductoParaEditar(String idProducto);

    /** CU-03 S1 · Guarda la edición; {@code archivos} son las imágenes nuevas (puede ir vacío). */
    ProductoEdicionDTO editarProducto(String idProducto, EditarProductoDTO dto, List<ArchivoImagen> archivos);

    ResultadoBajaDTO darDeBajaProducto(String idProducto);
}
