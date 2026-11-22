package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.persistencia.entidades.Empleado;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz Business Object para Empleado
 * Expone operaciones de negocio relacionadas con empleados
 *
 * SOLID: DIP - API layer depende de esta abstracción, no de DAOs
 */
public interface IEmpleadoBO {

    /**
     * Busca un empleado por su auth_id de Supabase
     * @param authId UUID del usuario en Supabase Auth
     * @return Optional con el empleado si existe
     */
    Optional<Empleado> findByAuthId(UUID authId);
}
