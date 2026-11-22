package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.ICodigoDescuentoDAO;
import com.flystore.persistencia.entidades.CodigoDescuento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/** DAO de CodigoDescuento. */
@Repository
@Transactional
public class CodigoDescuentoDAO implements ICodigoDescuentoDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<CodigoDescuento> obtenerPorCodigo(String codigo) {
        return entityManager.createQuery("select c from CodigoDescuento c where lower(trim(c.codigo)) = :codigo", CodigoDescuento.class)
                .setParameter("codigo", codigo.trim().toLowerCase(java.util.Locale.ROOT)).getResultStream().findFirst();
    }

    @Override
    public Optional<CodigoDescuento> bloquearPorId(Long id) {
        var codigo = entityManager.find(CodigoDescuento.class, id);
        if (codigo == null) return Optional.empty();
        // refresh con bloqueo: relee los usos vigentes aunque el código ya estuviera en el contexto.
        entityManager.refresh(codigo, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(codigo);
    }

    @Override
    public List<Long> obtenerIdsProductosAplicables(Long codigoDescuentoId) {
        return entityManager.createQuery("select pc.producto.id from ProductoCodigoDescuento pc where pc.codigoDescuento.id = :codigo", Long.class)
                .setParameter("codigo", codigoDescuentoId).getResultList();
    }
}
