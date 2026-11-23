package com.flystore.persistencia.dao;

import com.flystore.persistencia.cambios.CambiosProducto;
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
        return consultar(texto, idCategoria, idSubcategoria, idMarca, estado, null, null);
    }

    @Override
    public List<Producto> buscarPagina(String texto, Long idCategoria, Long idSubcategoria, Long idMarca, EstadoProducto estado, int pagina, int tamano) {
        return consultar(texto, idCategoria, idSubcategoria, idMarca, estado, pagina, tamano);
    }

    private List<Producto> consultar(String texto, Long idCategoria, Long idSubcategoria, Long idMarca, EstadoProducto estado, Integer pagina, Integer tamano) {
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
        if (pagina != null) query.setFirstResult(Math.multiplyExact(pagina, tamano)).setMaxResults(tamano + 1);
        var productos = query.getResultList();
        if (pagina != null && !productos.isEmpty()) {
            var ids = productos.stream().map(Producto::getId).toList();
            // Cargar cada colección por separado evita N+1 y paginar sobre joins de colecciones.
            entityManager.createQuery("select distinct p from Producto p left join fetch p.imagenes where p.id in :ids", Producto.class).setParameter("ids", ids).getResultList();
            entityManager.createQuery("select distinct p from Producto p left join fetch p.precios where p.id in :ids", Producto.class).setParameter("ids", ids).getResultList();
            entityManager.createQuery("select distinct p from Producto p left join fetch p.variantes v left join fetch v.talla left join fetch v.color where p.id in :ids", Producto.class).setParameter("ids", ids).getResultList();
        }
        return productos;
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
        var colores = new HashMap<String, Color>();
        var tallas = new HashMap<String, Talla>();
        Long idCategoria = subcategoria.getCategoria().getId();
        for (var variante : producto.getVariantes()) {
            variante.setColor(color(variante.getColor().getNombreColor(), colores));
            variante.setTalla(talla(variante.getTalla().getEtiqueta(), idCategoria, tallas));
            variante.setProducto(producto);
        }
        for (var imagen : producto.getImagenes()) {
            // La imagen se liga al color por nombre, igual que las variantes.
            if (imagen.getColor() != null) imagen.setColor(color(imagen.getColor().getNombreColor(), colores));
            imagen.setProducto(producto);
        }
        producto.getPrecios().forEach(p -> p.setProducto(producto));
        entityManager.persist(producto);
        entityManager.flush();
    }

    /** Color del catálogo por nombre, sin distinguir mayúsculas; no crea colores. */
    private Color color(String nombre, Map<String, Color> resueltos) {
        String clave = nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
        var conocido = resueltos.get(clave);
        if (conocido != null) return conocido;
        var colores = entityManager.createQuery("select c from Color c where lower(c.nombreColor) = :nombre", Color.class)
                .setParameter("nombre", clave).setMaxResults(2).getResultList();
        if (colores.size() != 1) throw new DatosCatalogoInvalidosException("Selecciona un color existente y sin duplicados en el catálogo");
        resueltos.put(clave, colores.getFirst());
        return colores.getFirst();
    }

    /** Talla por etiqueta dentro de la línea, sin distinguir mayúsculas; no crea tallas. */
    private Talla talla(String etiqueta, Long idCategoria, Map<String, Talla> resueltas) {
        String clave = etiqueta == null ? "" : etiqueta.trim().toLowerCase(Locale.ROOT);
        var conocida = resueltas.get(clave);
        if (conocida != null) return conocida;
        var tallas = entityManager.createQuery("select t from Talla t where lower(t.etiqueta) = :nombre and t.tipoTalla.categoria.id = :categoria", Talla.class)
                .setParameter("nombre", clave)
                .setParameter("categoria", idCategoria).setMaxResults(2).getResultList();
        if (tallas.size() != 1) throw new DatosCatalogoInvalidosException("Selecciona una talla válida y sin duplicados para la categoría");
        resueltas.put(clave, tallas.getFirst());
        return tallas.getFirst();
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

    // ===== CU-03 Editar producto =====

    @Override
    public Optional<Producto> obtenerParaEditar(Long idProducto) {
        // Sin bloqueo: el GET del formulario corre en una transacción de solo lectura y PostgreSQL rechaza
        // SELECT … FOR UPDATE ahí (error 25006). El PUT bloquea antes con obtenerPorId.
        var producto = entityManager.find(Producto.class, idProducto);
        if (producto == null) return Optional.empty();
        var ids = List.of(idProducto);
        // Una consulta por colección (las listas no se pueden traer juntas); llenan el mismo objeto.
        entityManager.createQuery("select p from Producto p join fetch p.subcategoria s join fetch s.categoria join fetch p.marca where p.id in :ids", Producto.class)
                .setParameter("ids", ids).getResultList();
        entityManager.createQuery("select distinct p from Producto p left join fetch p.precios where p.id in :ids", Producto.class)
                .setParameter("ids", ids).getResultList();
        entityManager.createQuery("select distinct p from Producto p left join fetch p.imagenes i left join fetch i.color where p.id in :ids", Producto.class)
                .setParameter("ids", ids).getResultList();
        entityManager.createQuery("select distinct p from Producto p left join fetch p.variantes v left join fetch v.color left join fetch v.talla where p.id in :ids", Producto.class)
                .setParameter("ids", ids).getResultList();
        return Optional.of(producto);
    }

    @Override
    public Set<Long> variantesConPedidos(Long idProducto) {
        return new HashSet<>(entityManager.createQuery(
                "select distinct d.varianteProducto.id from DetallePedidoVarianteProducto d where d.varianteProducto.producto.id = :id", Long.class)
                .setParameter("id", idProducto).getResultList());
    }

    @Override
    public boolean existeOtroConClave(String clave, Long idProducto) {
        return entityManager.createQuery("select count(p) from Producto p where lower(p.clave) = lower(:clave) and p.id <> :id", Long.class)
                .setParameter("clave", clave.trim()).setParameter("id", idProducto).getSingleResult() > 0;
    }

    @Override
    public void actualizar(Long idProducto, CambiosProducto cambios) {
        var producto = entityManager.find(Producto.class, idProducto);
        if (producto == null) throw new EntityNotFoundException("Producto no encontrado");
        Long idCategoria = producto.getSubcategoria().getCategoria().getId();

        // 1) Resolver y validar todo antes de tocar el producto: las consultas no ven cambios a medias.
        var subcategoria = entityManager.find(Subcategoria.class, cambios.idSubcategoria());
        var marca = entityManager.find(Marca.class, cambios.idMarca());
        if (subcategoria == null || marca == null) throw new EntityNotFoundException("Subcategoría o marca no encontrada");
        if (!subcategoria.getCategoria().getId().equals(idCategoria))
            throw new DatosCatalogoInvalidosException("El tipo debe ser de la misma línea del producto");
        if (!marca.getId().equals(producto.getMarca().getId()) && !Boolean.TRUE.equals(marca.getActivo()))
            throw new DatosCatalogoInvalidosException("La marca no está activa");
        var variantesActuales = new HashSet<Long>();
        producto.getVariantes().forEach(v -> variantesActuales.add(v.getId()));
        var imagenesActuales = new HashMap<Long, ImagenProducto>();
        producto.getImagenes().forEach(i -> imagenesActuales.put(i.getId(), i));
        if (!variantesActuales.containsAll(cambios.existencias().keySet()))
            throw new DatosCatalogoInvalidosException("Una de las variantes no pertenece al producto");
        var colores = new HashMap<String, Color>();
        var tallas = new HashMap<String, Talla>();
        for (var nueva : cambios.variantesNuevas()) {
            nueva.setColor(color(nueva.getColor().getNombreColor(), colores));
            nueva.setTalla(talla(nueva.getTalla().getEtiqueta(), idCategoria, tallas));
        }
        var coloresImagen = new ArrayList<Color>();
        for (var imagen : cambios.imagenes()) {
            if (imagen.idImagen() != null && !imagenesActuales.containsKey(imagen.idImagen()))
                throw new DatosCatalogoInvalidosException("Una de las imágenes no pertenece al producto");
            coloresImagen.add(imagen.color() == null ? null : color(imagen.color(), colores));
        }

        // 2) Aplicar. orphanRemoval borra las variantes, imágenes y precios que se quitan de las listas.
        producto.setClave(cambios.clave());
        producto.setNombre(cambios.nombre());
        producto.setDescripcion(cambios.descripcion());
        producto.setSubcategoria(subcategoria);
        producto.setMarca(marca);
        producto.setEstadoProducto(cambios.estado());

        producto.getVariantes().removeIf(v -> !cambios.existencias().containsKey(v.getId()));
        producto.getVariantes().forEach(v -> v.setStock(cambios.existencias().get(v.getId())));
        for (var nueva : cambios.variantesNuevas()) {
            nueva.setProducto(producto);
            producto.getVariantes().add(nueva);
        }

        var conservadas = new HashSet<Long>();
        cambios.imagenes().forEach(i -> { if (i.idImagen() != null) conservadas.add(i.idImagen()); });
        producto.getImagenes().removeIf(i -> !conservadas.contains(i.getId()));
        for (int orden = 0; orden < cambios.imagenes().size(); orden++) {
            var cambio = cambios.imagenes().get(orden);
            var imagen = cambio.idImagen() != null ? imagenesActuales.get(cambio.idImagen()) : new ImagenProducto();
            if (cambio.idImagen() == null) {
                imagen.setUrlImagen(cambio.url());
                imagen.setProducto(producto);
                producto.getImagenes().add(imagen);
            }
            imagen.setColor(coloresImagen.get(orden));
            imagen.setOrden(orden);
        }

        producto.getPrecios().clear();
        for (var precio : cambios.precios()) {
            precio.setProducto(producto);
            producto.getPrecios().add(precio);
        }
        entityManager.flush();
    }

    @Override
    public boolean imagenEnUso(String url) {
        return entityManager.createQuery("select count(i) from ImagenProducto i where i.urlImagen = :url", Long.class)
                .setParameter("url", url).getSingleResult() > 0;
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
