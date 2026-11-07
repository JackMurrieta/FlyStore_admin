package com.flystore.features.iniciarSesion.repository;

import com.flystore.entidades.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Empleado
 * Feature: Iniciar Sesión
 */
@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /**
     * Busca un empleado por su auth_id de Supabase
     */
    Optional<Empleado> findByAuthId(UUID authId);

    /**
     * Busca un empleado por su email
     */
    Optional<Empleado> findByEmail(String email);
}
