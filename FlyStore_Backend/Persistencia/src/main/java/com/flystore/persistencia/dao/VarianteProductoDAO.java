package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IVarianteProductoDAO;
import com.flystore.persistencia.entidades.VarianteProducto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/** DAO de VarianteProducto. */
@Repository
@Transactional
public class VarianteProductoDAO implements IVarianteProductoDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<VarianteProducto> obtenerPorId(Long id) {
        return entityManager.createQuery("select v from VarianteProducto v join fetch v.producto where v.id = :id", VarianteProducto.class)
                .setParameter("id", id).getResultStream().findFirst();
    }

    @Override
    public Optional<VarianteProducto> bloquearPorId(Long id) {
        var variante = entityManager.find(VarianteProducto.class, id);
        if (variante == null) return Optional.empty();
        // refresh con bloqueo: si la variante ya estaba en el contexto, relee el stock vigente.
        entityManager.refresh(variante, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(variante);
    }
}
