package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.ITipoClienteDAO;

import com.flystore.persistencia.entidades.TipoCliente;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

/**
 * Implementación DAO para TipoCliente
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
