package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.Admin;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz DAO para Admin
 */
public interface IAdminDAO {

    Optional<Admin> findByAuthId(UUID authId);

    Admin save(Admin admin);
}
