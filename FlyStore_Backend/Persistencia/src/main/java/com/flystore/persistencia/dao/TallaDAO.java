package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.ITallaDAO;
import com.flystore.persistencia.entidades.*;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class TallaDAO implements ITallaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Talla> obtenerPorCategoria(Long idCategoria) {
        if (entityManager.find(Categoria.class, idCategoria) == null) {
            throw new EntityNotFoundException("Categoría no encontrada");
        }
        return entityManager.createQuery("select t from Talla t where t.tipoTalla.categoria.id = :id order by t.id", Talla.class)
                .setParameter("id", idCategoria).getResultList();
    }
}
