package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IDetallePedidoDAO;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@Transactional(readOnly = true)
public class DetallePedidoDAO implements IDetallePedidoDAO {
    @PersistenceContext private EntityManager entityManager;
    @Override public boolean existenVentasDeProducto(Long idProducto) {
        return entityManager.createQuery("select count(d) from DetallePedidoVarianteProducto d where d.varianteProducto.producto.id = :id", Long.class)
                .setParameter("id", idProducto).getSingleResult() > 0;
    }
}
