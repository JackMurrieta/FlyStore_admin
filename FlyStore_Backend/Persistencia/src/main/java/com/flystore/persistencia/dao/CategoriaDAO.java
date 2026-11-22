package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.ICategoriaDAO;
import com.flystore.persistencia.entidades.Categoria;
import jakarta.persistence.*;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@Transactional(readOnly = true)
public class CategoriaDAO implements ICategoriaDAO {
    @PersistenceContext private EntityManager entityManager;
    @Override public List<Categoria> obtenerTodas() {
        return entityManager.createQuery("select e from Categoria e order by e.nombre, e.id", Categoria.class).getResultList();
    }

    @Override public Optional<Categoria> obtenerPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return Optional.empty();
        return entityManager.createQuery("select e from Categoria e where lower(trim(e.nombre)) = :nombre order by e.id", Categoria.class)
                .setParameter("nombre", nombre.trim().toLowerCase(Locale.ROOT))
                .setMaxResults(1).getResultList().stream().findFirst();
    }
}
