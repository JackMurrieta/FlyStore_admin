package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.Empleado;
import com.flystore.features.iniciarSesion.repository.EmpleadoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación DAO para Empleado
 */
@Repository
public class EmpleadoDAO implements IEmpleadoDAO {

    private final EmpleadoRepository repository;

    public EmpleadoDAO(EmpleadoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Empleado> findByAuthId(UUID authId) {
        return repository.findByAuthId(authId);
    }

    @Override
    public Empleado save(Empleado empleado) {
        return repository.save(empleado);
    }
}
