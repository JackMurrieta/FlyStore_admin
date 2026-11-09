package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.TipoCliente;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

/**
 * Implementación DAO para TipoCliente
 * SOLID: SRP - Solo responsable de acceso a tipos de cliente
 * GRASP: Pure Fabrication - Clase creada para mantener arquitectura en capas
 *
 * Nota: Como TipoCliente es un enum (no una tabla), esta clase simplemente
 * devuelve los valores del enum. Si en el futuro TipoCliente viene de BD,
 * aquí se agregaría el repository correspondiente.
 */
@Repository
public class TipoClienteDAO implements ITipoClienteDAO {

    /**
     * Obtiene todos los tipos de cliente del enum TipoCliente
     * En el futuro, si los tipos vienen de BD, aquí se consultaría el repository
     */
    @Override
    public List<TipoCliente> obtenerTodos() {
        return Arrays.asList(TipoCliente.values());
    }
}
