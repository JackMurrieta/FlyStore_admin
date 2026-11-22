package com.flystore.persistencia.dao;
import com.flystore.persistencia.dao.interfaces.IColorDAO;
import com.flystore.persistencia.entidades.Color;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
@Transactional(readOnly = true)
public class ColorDAO implements IColorDAO {
 @PersistenceContext private EntityManager entityManager;
 public List<Color> obtenerTodos() {
 return entityManager.createQuery("select c from Color c order by c.nombreColor", Color.class).getResultList();
 }
}
