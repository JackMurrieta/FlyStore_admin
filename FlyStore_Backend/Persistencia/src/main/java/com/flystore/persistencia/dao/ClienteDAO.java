package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IClienteDAO;

import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;
import com.flystore.persistencia.repositorios.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación DAO para Cliente usando Spring Data JPA Repository
 */
@Repository
public class ClienteDAO implements IClienteDAO {

    private final ClienteRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

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

    @Override
    public Optional<Cliente> obtenerPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    public Optional<Cliente> bloquearPorId(Long id) {
        return Optional.ofNullable(entityManager.find(Cliente.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public void guardarDatosEntrega(DatosEntrega datosEntrega) {
        entityManager.persist(datosEntrega);
    }
}
