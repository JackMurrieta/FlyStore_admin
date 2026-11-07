package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.Empleado;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz DAO para Empleado
 */
public interface IEmpleadoDAO {

    Optional<Empleado> findByAuthId(UUID authId);

    Empleado save(Empleado empleado);
}
