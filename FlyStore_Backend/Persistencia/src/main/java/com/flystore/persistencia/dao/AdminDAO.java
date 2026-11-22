package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IAdminDAO;

import com.flystore.persistencia.entidades.Admin;
import com.flystore.persistencia.repositorios.AdminRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación DAO para Admin
 */
@Repository
public class AdminDAO implements IAdminDAO {

    private final AdminRepository repository;

    public AdminDAO(AdminRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Admin> findByAuthId(UUID authId) {
        return repository.findByAuthId(authId);
    }

    @Override
    public Admin save(Admin admin) {
        return repository.save(admin);
    }
}
