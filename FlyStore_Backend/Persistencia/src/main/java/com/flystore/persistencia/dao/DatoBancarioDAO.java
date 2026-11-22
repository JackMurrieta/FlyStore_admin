package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IDatoBancarioDAO;
import com.flystore.persistencia.entidades.DatoBancario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/** DAO de DatoBancario. */
@Repository
@Transactional(readOnly = true)
public class DatoBancarioDAO implements IDatoBancarioDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<DatoBancario> obtenerDatoBancarioActivo() {
        return entityManager.createQuery("select d from DatoBancario d where d.activo = true order by d.id", DatoBancario.class)
                .setMaxResults(1).getResultStream().findFirst();
    }
}
