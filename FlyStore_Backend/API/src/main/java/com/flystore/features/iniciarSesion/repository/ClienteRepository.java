package com.flystore.features.iniciarSesion.repository;

import com.flystore.entidades.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Cliente
 * Feature: Iniciar Sesión
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca un cliente por su auth_id de Supabase
     */
    Optional<Cliente> findByAuthId(UUID authId);

    /**
     * Busca un cliente por su email
     */
    Optional<Cliente> findByEmail(String email);
}
