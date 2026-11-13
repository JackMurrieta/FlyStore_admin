package com.flystore.persistencia.dao;

import com.flystore.persistencia.dao.interfaces.IProductoDAO;
import com.flystore.persistencia.entidades.*;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class ProductoDAO implements IProductoDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Producto> obtenerTodos() {
        return buscar(null, null, null, null, null);
    }

    @Override
    public List<Producto> buscar(String texto, Long idCategoria, Long idSubcategoria, Long idMarca, EstadoProducto estado) {
        var jpql = new StringBuilder("select p from Producto p join fetch p.subcategoria s join fetch s.categoria join fetch p.marca where 1=1");
        var params = new HashMap<String, Object>();
        if (texto != null && !texto.isBlank()) {
            jpql.append(" and (locate(:texto, lower(p.nombre)) > 0 or locate(:texto, lower(p.clave)) > 0)");
            params.put("texto", texto.trim().toLowerCase(Locale.ROOT));
        }
        if (idCategoria != null) {
            jpql.append(" and s.categoria.id = :categoria");
            params.put("categoria", idCategoria);
        }
        if (idSubcategoria != null) {
            jpql.append(" and s.id = :subcategoria");
            params.put("subcategoria", idSubcategoria);
        }
        if (idMarca != null) {
            jpql.append(" and p.marca.id = :marca");
            params.put("marca", idMarca);
        }
        if (estado != null) {
            jpql.append(" and p.estadoProducto = :estado");
            params.put("estado", estado);
        }
        var query = entityManager.createQuery(jpql.append(" order by p.id").toString(), Producto.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public Optional<Producto> obtenerPorId(Long idProducto) {
        return Optional.ofNullable(entityManager.find(Producto.class, idProducto, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public boolean existePorClave(String clave) {
        return entityManager.createQuery("select count(p) from Producto p where lower(p.clave) = lower(:clave)", Long.class)
                .setParameter("clave", clave.trim()).getSingleResult() > 0;
    }

    @Override
    public void guardar(Producto producto) {
        var solicitada = producto.getSubcategoria();
        var subcategoria = entityManager.find(Subcategoria.class, solicitada.getId());
        var marca = entityManager.find(Marca.class, producto.getMarca().getId());
        if (subcategoria == null || marca == null) {
            throw new EntityNotFoundException("Subcategoría o marca no encontrada");
        }
        if (!subcategoria.getCategoria().getId().equals(solicitada.getCategoria().getId())) {
            throw new IllegalArgumentException("La subcategoría no pertenece a la categoría seleccionada");
        }
        if (!Boolean.TRUE.equals(marca.getActivo())) {
            throw new IllegalArgumentException("La marca no está activa");
        }
        producto.setSubcategoria(subcategoria);
        producto.setMarca(marca);
        for (var variante : producto.getVariantes()) {
            var colores = entityManager.createQuery("select c from Color c where lower(c.nombreColor) = :nombre", Color.class)
                    .setParameter("nombre", variante.getColor().getNombreColor().toLowerCase(Locale.ROOT)).setMaxResults(2).getResultList();
            var tallas = entityManager.createQuery("select t from Talla t where lower(t.etiqueta) = :nombre and t.tipoTalla.categoria.id = :categoria", Talla.class)
                    .setParameter("nombre", variante.getTalla().getEtiqueta().toLowerCase(Locale.ROOT))
                    .setParameter("categoria", subcategoria.getCategoria().getId()).setMaxResults(2).getResultList();
            if (colores.size() != 1 || tallas.size() != 1) {
                throw new IllegalArgumentException("Talla o color inexistente, ambiguo o ajeno a la categoría");
            }
            variante.setColor(colores.getFirst());
            variante.setTalla(tallas.getFirst());
            variante.setProducto(producto);
        }
        producto.getPrecios().forEach(p -> p.setProducto(producto));
        producto.getImagenes().forEach(i -> i.setProducto(producto));
        entityManager.persist(producto);
        entityManager.flush();
    }

    @Override
    public void cambiarEstado(Long idProducto, EstadoProducto estado) {
        var producto = obtenerPorId(idProducto).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        producto.setEstadoProducto(estado);
    }

    @Override
    public void eliminar(Long idProducto) {
        var producto = obtenerPorId(idProducto).orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
        entityManager.remove(producto);
        entityManager.flush();
    }
}
