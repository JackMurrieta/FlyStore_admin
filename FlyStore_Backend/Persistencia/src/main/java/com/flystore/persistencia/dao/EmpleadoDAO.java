package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;

import com.flystore.persistencia.entidades.Empleado;
import com.flystore.persistencia.repositorios.EmpleadoRepository;
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
