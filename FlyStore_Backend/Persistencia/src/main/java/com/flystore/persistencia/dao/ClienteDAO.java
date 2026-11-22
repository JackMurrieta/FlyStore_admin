package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IClienteDAO;

import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.repositorios.ClienteRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación DAO para Cliente usando Spring Data JPA Repository
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
