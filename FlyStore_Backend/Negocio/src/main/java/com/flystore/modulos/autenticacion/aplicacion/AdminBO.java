package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import com.flystore.persistencia.entidades.Admin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Business Object para Admin
 * Implementa lógica de negocio relacionada con administradores
 *
 * SOLID: SRP - Responsable únicamente de operaciones de negocio de administradores
 * SOLID: DIP - Depende de abstracción IAdminDAO
 */
@Service
@Transactional(readOnly = true)
public class AdminBO implements IAdminBO {

    private final IAdminDAO adminDAO;

    public AdminBO(IAdminDAO adminDAO) {
        this.adminDAO = adminDAO;
    }

    @Override
    public Optional<Admin> findByAuthId(UUID authId) {
        return adminDAO.findByAuthId(authId);
    }
}
