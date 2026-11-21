package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.entidades.Empleado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Business Object para Empleado
 * Implementa lógica de negocio relacionada con empleados
 *
 * SOLID: SRP - Responsable únicamente de operaciones de negocio de empleados
 * SOLID: DIP - Depende de abstracción IEmpleadoDAO
 */
@Service
@Transactional(readOnly = true)
public class EmpleadoBO implements IEmpleadoBO {

    private final IEmpleadoDAO empleadoDAO;

    public EmpleadoBO(IEmpleadoDAO empleadoDAO) {
        this.empleadoDAO = empleadoDAO;
    }

    @Override
    public Optional<Empleado> findByAuthId(UUID authId) {
        return empleadoDAO.findByAuthId(authId);
    }
}
