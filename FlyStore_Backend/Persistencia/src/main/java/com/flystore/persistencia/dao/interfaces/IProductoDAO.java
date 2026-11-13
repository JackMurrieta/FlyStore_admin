package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.*;
import java.util.List;
import java.util.Optional;
public interface IProductoDAO {
    List<Producto> obtenerTodos();
    List<Producto> buscar(String texto, Long idCategoria, Long idSubcategoria, Long idMarca, EstadoProducto estado);
    Optional<Producto> obtenerPorId(Long idProducto);
    boolean existePorClave(String clave);
    void guardar(Producto producto);
    void cambiarEstado(Long idProducto, EstadoProducto estado);
    void eliminar(Long idProducto);
}
