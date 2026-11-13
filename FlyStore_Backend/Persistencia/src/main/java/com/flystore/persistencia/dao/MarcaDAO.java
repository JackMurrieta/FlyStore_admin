package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IMarcaDAO;
import com.flystore.persistencia.entidades.Marca;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@Transactional(readOnly = true)
public class MarcaDAO implements IMarcaDAO {
    @PersistenceContext private EntityManager entityManager;
    @Override public List<Marca> obtenerTodas() {
        return entityManager.createQuery("select e from Marca e order by e.nombre, e.id", Marca.class).getResultList();
    }
}
