package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.TipoCliente;

import java.util.List;

/**
 * Interfaz DAO para acceso a tipos de cliente
 * SOLID: ISP - Interfaz específica para operaciones de TipoCliente
 * GRASP: Information Expert - Conoce cómo obtener los tipos de cliente
 */
public interface ITipoClienteDAO {

    /**
     * Obtiene todos los tipos de cliente disponibles
     * @return Lista de tipos de cliente (enum)
     */
    List<TipoCliente> obtenerTodos();
}
