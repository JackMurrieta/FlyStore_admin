package com.flystore.features.iniciarSesion.DAO;

import com.flystore.entidades.Cliente;
import com.flystore.features.iniciarSesion.repository.ClienteRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación DAO para Cliente usando Spring Data JPA Repository
 * SOLID: SRP - Solo responsable de persistencia de Cliente
 * GRASP: Pure Fabrication - Clase técnica para acceso a datos
 */
@Repository
public class ClienteDAO implements IClienteDAO {

    private final ClienteRepository repository;

    public ClienteDAO(ClienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Cliente> findByAuthId(UUID authId) {
        return repository.findByAuthId(authId);
    }

    @Override
    public Cliente save(Cliente cliente) {
        return repository.save(cliente);
    }
}
