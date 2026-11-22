package com.flystore.persistencia.repositorios;

import com.flystore.persistencia.entidades.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Admin
 * Feature: Iniciar Sesión
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Busca un admin por su auth_id de Supabase
     */
    Optional<Admin> findByAuthId(UUID authId);

    /**
     * Busca un admin por su email
     */
    Optional<Admin> findByEmail(String email);
}
