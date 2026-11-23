package com.flystore.persistencia.dao;

import com.flystore.persistencia.consultas.CriteriosBusquedaPedidos;
import com.flystore.persistencia.consultas.PaginaPedidos;
import com.flystore.persistencia.dao.interfaces.IPedidoVentaDAO;
import com.flystore.persistencia.entidades.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * DAO de PedidoVenta con EntityManager. Solo consulta y escribe; las reglas viven en los BO.
 * Las consultas están en una sola línea para que MapeoJpaTest las valide sin base de datos.
 */
@Repository
@Transactional
public class PedidoVentaDAO implements IPedidoVentaDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PedidoVenta crearPedidoVenta(Long clienteId) {
        var pedido = new PedidoVenta();
        pedido.setCliente(entityManager.getReference(Cliente.class, clienteId));
        pedido.setEstadoVenta(EstadoVenta.PENDIENTE);
        pedido.setFechaHoraPedido(OffsetDateTime.now());
        pedido.setDetalles(new ArrayList<>());
        entityManager.persist(pedido);
        // flush inmediato: si otra petición ya creó el carrito, el índice único falla aquí.
        entityManager.flush();
        return pedido;
    }

    @Override
    public Optional<PedidoVenta> obtenerPedidoPendienteCliente(Long clienteId) {
        return entityManager.createQuery("select p from PedidoVenta p where p.cliente.id = :cliente and p.estadoVenta = :estado", PedidoVenta.class)
                .setParameter("cliente", clienteId).setParameter("estado", EstadoVenta.PENDIENTE)
                .getResultStream().findFirst();
    }

    @Override
    public Optional<PedidoVenta> bloquearPedidoPendienteCliente(Long clienteId) {
        return entityManager.createQuery("select p from PedidoVenta p where p.cliente.id = :cliente and p.estadoVenta = :estado", PedidoVenta.class)
                .setParameter("cliente", clienteId).setParameter("estado", EstadoVenta.PENDIENTE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst();
    }

    @Override
    public Optional<PedidoVenta> bloquearPorId(Long idPedido) {
        return entityManager.createQuery("select p from PedidoVenta p where p.id = :id", PedidoVenta.class)
                .setParameter("id", idPedido).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst();
    }

    @Override
    public Optional<PedidoVenta> bloquearPorFolio(String folio) {
        return entityManager.createQuery("select p from PedidoVenta p where p.folio = :folio", PedidoVenta.class)
                .setParameter("folio", folio).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst();
    }

    @Override
    public List<PedidoVenta> listarPorEstados(Collection<EstadoVenta> estados) {
        return entityManager.createQuery("select p from PedidoVenta p join fetch p.cliente left join fetch p.pago where p.estadoVenta in :estados", PedidoVenta.class)
                .setParameter("estados", estados).getResultList();
    }

    @Override
    public Optional<PedidoVenta> obtenerPorIdCompleto(Long idPedido) {
        var pedido = entityManager.createQuery("select p from PedidoVenta p join fetch p.cliente left join fetch p.datosEntrega left join fetch p.pago where p.id = :id", PedidoVenta.class)
                .setParameter("id", idPedido).getResultStream().findFirst();
        pedido.ifPresent(this::cargarLineas);
        return pedido;
    }

    @Override
    public Optional<PedidoVenta> obtenerPorFolioCompleto(String folio) {
        var pedido = entityManager.createQuery("select p from PedidoVenta p join fetch p.cliente left join fetch p.datosEntrega left join fetch p.pago where p.folio = :folio", PedidoVenta.class)
                .setParameter("folio", folio).getResultStream().findFirst();
        pedido.ifPresent(this::cargarLineas);
        return pedido;
    }

    /**
     * Carga en el contexto de persistencia las líneas con su variante. Solo se hace fetch de una
     * colección por consulta (Hibernate no permite dos bags en la misma consulta).
     */
    private void cargarLineas(PedidoVenta pedido) {
        entityManager.createQuery("select distinct d from DetallePedido d left join fetch d.variantes dv left join fetch dv.varianteProducto vp left join fetch vp.producto pr left join fetch pr.marca left join fetch vp.color left join fetch vp.talla t left join fetch t.tipoTalla where d.pedidoVenta.id = :pedido", DetallePedido.class)
                .setParameter("pedido", pedido.getId()).getResultList();
    }

    @Override
    public List<PedidoVenta> listarRegistrados() {
        return entityManager.createQuery("select p from PedidoVenta p join fetch p.cliente where p.estadoVenta <> :pendiente order by p.fechaHoraPedido desc", PedidoVenta.class)
                .setParameter("pendiente", EstadoVenta.PENDIENTE).getResultList();
    }

    @Override
    public List<PedidoVenta> listarRegistradosPorCliente(Long clienteId) {
        return entityManager.createQuery("select p from PedidoVenta p join fetch p.cliente where p.cliente.id = :cliente and p.estadoVenta <> :pendiente order by p.fechaHoraPedido desc", PedidoVenta.class)
                .setParameter("cliente", clienteId).setParameter("pendiente", EstadoVenta.PENDIENTE).getResultList();
    }

    @Override
    public List<PedidoVenta> listarPorEstadosRegistradosAntesDe(Collection<EstadoVenta> estados, OffsetDateTime limite, OffsetDateTime ahora) {
        return entityManager.createQuery("select p from PedidoVenta p where p.estadoVenta in :estados and ((p.fechaLimitePago is null and p.fechaHoraPedido < :limite) or p.fechaLimitePago < :ahora)", PedidoVenta.class)
                .setParameter("estados", estados).setParameter("limite", limite).setParameter("ahora", ahora)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    // ===== CU-04 Administrar pedidos =====

    /** Consultas base de la búsqueda; los filtros se agregan en {@link #consultaBusqueda}. */
    private static final String BUSCAR = "select p from PedidoVenta p join fetch p.cliente left join fetch p.datosEntrega left join fetch p.pago pg where p.estadoVenta <> :pendiente";
    private static final String CONTAR = "select count(p) from PedidoVenta p left join p.pago pg where p.estadoVenta <> :pendiente";

    @Override
    public PaginaPedidos buscarPorCriterios(CriteriosBusquedaPedidos criterios, List<Long> clienteIds) {
        var parametros = new LinkedHashMap<String, Object>();
        parametros.put("pendiente", EstadoVenta.PENDIENTE);
        if (criterios.texto() != null) {
            parametros.put("folio", "%" + criterios.texto().trim().toUpperCase(Locale.ROOT) + "%");
            if (clienteIds != null && !clienteIds.isEmpty()) parametros.put("clientes", clienteIds);
        }
        if (criterios.estado() != null) parametros.put("estado", criterios.estado());
        if (criterios.desde() != null) parametros.put("desde", criterios.desde());
        if (criterios.hasta() != null) parametros.put("hasta", criterios.hasta());

        var cuenta = entityManager.createQuery(consultaBusqueda(true, criterios, clienteIds), Long.class);
        parametros.forEach(cuenta::setParameter);
        long total = cuenta.getSingleResult();
        if (total == 0) return new PaginaPedidos(List.of(), 0);

        var pagina = entityManager.createQuery(consultaBusqueda(false, criterios, clienteIds), PedidoVenta.class);
        parametros.forEach(pagina::setParameter);
        var pedidos = pagina.setFirstResult((criterios.pagina() - 1) * CriteriosBusquedaPedidos.TAMANO_PAGINA)
                .setMaxResults(CriteriosBusquedaPedidos.TAMANO_PAGINA).getResultList();
        return new PaginaPedidos(pedidos, total);
    }

    /** HQL de la búsqueda (o de su conteo) con solo los filtros presentes. Visible para su prueba. */
    static String consultaBusqueda(boolean contar, CriteriosBusquedaPedidos criterios, List<Long> clienteIds) {
        var hql = new StringBuilder(contar ? CONTAR : BUSCAR);
        if (criterios.texto() != null) {
            // El texto busca en el folio y, si hubo clientes que coinciden con él, también en sus pedidos.
            hql.append(clienteIds != null && !clienteIds.isEmpty()
                    ? " and (upper(p.folio) like :folio or p.cliente.id in :clientes)"
                    : " and upper(p.folio) like :folio");
        }
        if (criterios.estado() != null) hql.append(" and p.estadoVenta = :estado");
        if (CriteriosBusquedaPedidos.EFECTIVO.equals(criterios.metodoPago())) hql.append(" and type(pg) = PagoEfectivo");
        else if (CriteriosBusquedaPedidos.TRANSFERENCIA.equals(criterios.metodoPago())) hql.append(" and type(pg) = PagoTransferencia");
        if (criterios.desde() != null) hql.append(" and p.fechaHoraPedido >= :desde");
        if (criterios.hasta() != null) hql.append(" and p.fechaHoraPedido < :hasta");
        if (!contar) hql.append(" order by p.fechaHoraPedido desc, p.id desc");
        return hql.toString();
    }

    @Override
    public void guardarPago(Pago pago) {
        entityManager.persist(pago);
    }

    @Override
    public void guardarPedidoVenta(PedidoVenta pedidoVenta) {
        if (pedidoVenta.getId() == null) entityManager.persist(pedidoVenta);
        else entityManager.merge(pedidoVenta);
        entityManager.flush();
    }
}
