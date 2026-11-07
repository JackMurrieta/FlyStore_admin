package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.Admin;
import com.flystore.features.iniciarSesion.repository.AdminRepository;
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
