package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.consultas.ConteoOpcion;
import com.flystore.persistencia.consultas.FiltroCatalogo;
import com.flystore.persistencia.consultas.OpcionesFiltroCatalogo;
import com.flystore.persistencia.consultas.PaginaProductos;
import com.flystore.persistencia.cambios.CambiosProducto;
import com.flystore.persistencia.entidades.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
public interface IProductoDAO {
    List<Producto> obtenerTodos();
    List<Producto> buscar(String texto, Long idCategoria, Long idSubcategoria, Long idMarca, EstadoProducto estado);
    List<Producto> buscarPagina(String texto, Long idCategoria, Long idSubcategoria, Long idMarca, EstadoProducto estado, int pagina, int tamano);
    Optional<Producto> obtenerPorId(Long idProducto);
    boolean existePorClave(String clave);
    void guardar(Producto producto);
    void cambiarEstado(Long idProducto, EstadoProducto estado);
    void eliminar(Long idProducto);

    // ===== CU-03 Editar producto =====

    /**
     * Producto para editarlo, con línea, marca, variantes (color y talla), imágenes (color) y precios ya cargados.
     * No bloquea (sirve también en transacciones de solo lectura); para guardar, bloquear antes con {@link #obtenerPorId}.
     */
    Optional<Producto> obtenerParaEditar(Long idProducto);

    /** Ids de las variantes del producto que están en algún carrito o pedido (no se pueden borrar). */
    Set<Long> variantesConPedidos(Long idProducto);

    /** Otro producto ya usa esta clave (sin distinguir mayúsculas ni espacios exteriores). */
    boolean existeOtroConClave(String clave, Long idProducto);

    /**
     * Aplica la edición al producto obtenido con {@link #obtenerParaEditar}: valida y resuelve tipo,
     * marca, tallas y colores (como en el alta) antes de cambiar nada, y quita lo que no viene.
     */
    void actualizar(Long idProducto, CambiosProducto cambios);

    /** Alguna imagen de cualquier producto usa todavía esta URL (para no borrar archivos compartidos). */
    boolean imagenEnUso(String url);

    // ===== CU-01 Consultar catálogo (tienda) =====

    /** Ids de una página de productos visibles que cumplen el filtro, en su orden, y el total. */
    PaginaProductos buscarEnTienda(FiltroCatalogo filtro, int desde, int cantidad);

    /**
     * Carga los productos con marca, subcategoría, línea, precios e imágenes (y variantes con
     * color, talla y tipo de talla si se pide) en pocas consultas, en el mismo orden de los ids.
     */
    List<Producto> obtenerParaTienda(List<Long> ids, boolean conVariantes);

    /** Producto visible en la tienda con todo lo que muestra su ficha; vacío si no se muestra. */
    Optional<Producto> obtenerVisibleEnTienda(Long idProducto);

    /** Opciones con conteos para llenar los filtros del alcance indicado. */
    OpcionesFiltroCatalogo obtenerOpcionesFiltro(FiltroCatalogo alcance);

    /** Productos visibles del alcance agrupados por línea (id, nombre y cantidad). */
    List<ConteoOpcion> contarPorLinea(FiltroCatalogo alcance);
}
