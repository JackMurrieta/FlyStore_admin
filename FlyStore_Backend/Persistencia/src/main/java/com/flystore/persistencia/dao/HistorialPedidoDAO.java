package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IHistorialPedidoDAO;
import com.flystore.persistencia.entidades.HistorialPedido;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** DAO de HistorialPedido con EntityManager. La consulta va en una sola línea para MapeoJpaTest. */
@Repository
@Transactional
public class HistorialPedidoDAO implements IHistorialPedidoDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public HistorialPedido guardar(HistorialPedido entrada) {
        entityManager.persist(entrada);
        return entrada;
    }

    @Override
    public List<HistorialPedido> listarPorPedido(Long pedidoId) {
        return entityManager.createQuery("select h from HistorialPedido h where h.pedidoVenta.id = :pedido order by h.fechaHora desc, h.id desc", HistorialPedido.class)
                .setParameter("pedido", pedidoId).getResultList();
    }
}
