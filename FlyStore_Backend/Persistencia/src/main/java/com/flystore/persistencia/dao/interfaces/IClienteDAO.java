package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.Cliente;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz DAO para Cliente
 * SOLID: ISP - Interfaz pequeña y específica
 * SOLID: DIP - Capa superior depende de abstracción
 */
public interface IClienteDAO {

    /**
     * Busca un cliente por su auth_id de Supabase
     * @param authId UUID del usuario en Supabase Auth
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> findByAuthId(UUID authId);

    /**
     * Guarda un cliente en la base de datos
     * @param cliente Cliente a guardar
     * @return Cliente guardado
     */
    Cliente save(Cliente cliente);
}
