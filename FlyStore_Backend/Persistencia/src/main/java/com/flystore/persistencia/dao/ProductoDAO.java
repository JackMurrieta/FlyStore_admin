package com.flystore.persistencia.dao;

import com.flystore.persistencia.consultas.*;
import com.flystore.persistencia.dao.interfaces.IProductoDAO;
import com.flystore.persistencia.entidades.*;
import jakarta.persistence.*;
import com.flystore.persistencia.excepcion.DatosCatalogoInvalidosException;
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
            throw new DatosCatalogoInvalidosException("La subcategoría no pertenece a la categoría seleccionada");
        }
        if (!Boolean.TRUE.equals(marca.getActivo())) {
            throw new DatosCatalogoInvalidosException("La marca no está activa");
        }
        producto.setSubcategoria(subcategoria);
        producto.setMarca(marca);
        for (var variante : producto.getVariantes()) {
            var colores = entityManager.createQuery("select c from Color c where lower(c.nombreColor) = :nombre", Color.class)
                    .setParameter("nombre", variante.getColor().getNombreColor().toLowerCase(Locale.ROOT)).setMaxResults(2).getResultList();
            var tallas = entityManager.createQuery("select t from Talla t where lower(t.etiqueta) = :nombre and t.tipoTalla.categoria.id = :categoria", Talla.class)
                    .setParameter("nombre", variante.getTalla().getEtiqueta().toLowerCase(Locale.ROOT))
                    .setParameter("categoria", subcategoria.getCategoria().getId()).setMaxResults(2).getResultList();
            if (colores.size() != 1) throw new DatosCatalogoInvalidosException("Selecciona un color existente y sin duplicados en el catálogo");
            if (tallas.size() != 1) throw new DatosCatalogoInvalidosException("Selecciona una talla válida y sin duplicados para la categoría");
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

    // ===== CU-01 Consultar catálogo (tienda) =====

    @Override
    @Transactional(readOnly = true)
    public PaginaProductos buscarEnTienda(FiltroCatalogo filtro, int desde, int cantidad) {
        var consulta = new ConsultaTiendaSql(filtro);
        long total = numero(consulta.aplicar(entityManager.createNativeQuery(consulta.contar())).getSingleResult());
        if (total == 0 || desde >= total) return new PaginaProductos(List.of(), total);
        var query = consulta.aplicar(entityManager.createNativeQuery(consulta.idsOrdenados()));
        query.setFirstResult(desde);
        query.setMaxResults(cantidad);
        var ids = ((List<?>) query.getResultList()).stream().map(ProductoDAO::numero).toList();
        return new PaginaProductos(ids, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> obtenerParaTienda(List<Long> ids, boolean conVariantes) {
        if (ids == null || ids.isEmpty()) return List.of();
        // Una consulta por colección: las listas (bags) no se pueden traer juntas en un solo join fetch.
        // Todas usan el mismo contexto de persistencia, así que llenan los mismos objetos Producto.
        var productos = entityManager.createQuery(
                "select p from Producto p join fetch p.subcategoria s join fetch s.categoria join fetch p.marca where p.id in :ids",
                Producto.class).setParameter("ids", ids).getResultList();
        entityManager.createQuery("select distinct p from Producto p left join fetch p.precios where p.id in :ids", Producto.class)
                .setParameter("ids", ids).getResultList();
        entityManager.createQuery(
                "select distinct p from Producto p left join fetch p.imagenes i left join fetch i.color where p.id in :ids",
                Producto.class).setParameter("ids", ids).getResultList();
        if (conVariantes) {
            entityManager.createQuery(
                    "select distinct p from Producto p left join fetch p.variantes v left join fetch v.color left join fetch v.talla t left join fetch t.tipoTalla where p.id in :ids",
                    Producto.class).setParameter("ids", ids).getResultList();
        }
        var porId = new HashMap<Long, Producto>();
        productos.forEach(p -> porId.put(p.getId(), p));
        return ids.stream().map(porId::get).filter(Objects::nonNull).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Producto> obtenerVisibleEnTienda(Long idProducto) {
        long visibles = numero(entityManager.createNativeQuery(ConsultaTiendaSql.esVisible())
                .setParameter("idProducto", idProducto).getSingleResult());
        if (visibles == 0) return Optional.empty();
        return obtenerParaTienda(List.of(idProducto), true).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public OpcionesFiltroCatalogo obtenerOpcionesFiltro(FiltroCatalogo alcance) {
        var consulta = new ConsultaTiendaSql(alcance);
        var marcas = filas(consulta, consulta.conteoMarcas()).stream()
                .map(f -> new ConteoMarca(numero(f[0]), (String) f[1], (String) f[2], numero(f[3]))).toList();
        var subcategorias = filas(consulta, consulta.conteoSubcategorias()).stream()
                .map(f -> new ConteoOpcion(numero(f[0]), (String) f[1], numero(f[2]))).toList();
        var colores = filas(consulta, consulta.conteoColores()).stream()
                .map(f -> new ConteoColor(numero(f[0]), (String) f[1], (String) f[2], numero(f[3]))).toList();
        var tallas = filas(consulta, consulta.conteoTallas()).stream()
                .map(f -> new ConteoTalla(numero(f[0]), (String) f[1], numero(f[2]), (String) f[3], (String) f[4], numero(f[5]))).toList();
        var rango = (Object[]) consulta.aplicarAlcance(entityManager.createNativeQuery(consulta.rangoPrecio())).getSingleResult();
        return new OpcionesFiltroCatalogo(marcas, subcategorias, colores, tallas, decimal(rango[0]), decimal(rango[1]));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConteoOpcion> contarPorLinea(FiltroCatalogo alcance) {
        var consulta = new ConsultaTiendaSql(alcance);
        return filas(consulta, consulta.conteoLineas()).stream()
                .map(f -> new ConteoOpcion(numero(f[0]), f[1] == null ? null : ((String) f[1]).trim(), numero(f[2]))).toList();
    }

    private List<Object[]> filas(ConsultaTiendaSql consulta, String sql) {
        var resultado = consulta.aplicarAlcance(entityManager.createNativeQuery(sql)).getResultList();
        return ((List<?>) resultado).stream().map(f -> (Object[]) f).toList();
    }

    private static long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private static java.math.BigDecimal decimal(Object valor) {
        if (valor == null) return null;
        return valor instanceof java.math.BigDecimal d ? d : new java.math.BigDecimal(valor.toString());
    }
}
