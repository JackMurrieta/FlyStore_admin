package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.persistencia.entidades.Admin;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz Business Object para Admin
 * Expone operaciones de negocio relacionadas con administradores
 *
 * SOLID: DIP - API layer depende de esta abstracción, no de DAOs
 */
public interface IAdminBO {

    /**
     * Busca un administrador por su auth_id de Supabase
     * @param authId UUID del usuario en Supabase Auth
     * @return Optional con el administrador si existe
     */
    Optional<Admin> findByAuthId(UUID authId);
}
