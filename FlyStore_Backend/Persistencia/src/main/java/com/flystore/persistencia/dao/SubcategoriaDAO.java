package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.ISubcategoriaDAO;
import com.flystore.persistencia.entidades.Subcategoria;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SubcategoriaDAO implements ISubcategoriaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Subcategoria> obtenerTodas() {
        return entityManager.createQuery("select e from Subcategoria e order by e.nombre, e.id", Subcategoria.class).getResultList();
    }
}
