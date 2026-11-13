package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.Admin;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz DAO para Admin
 */
public interface IAdminDAO {

    Optional<Admin> findByAuthId(UUID authId);

    Admin save(Admin admin);
}
