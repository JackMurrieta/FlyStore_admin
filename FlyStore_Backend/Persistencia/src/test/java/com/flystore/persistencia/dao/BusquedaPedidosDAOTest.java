package com.flystore.persistencia.dao;

import com.flystore.persistencia.consultas.CriteriosBusquedaPedidos;
import com.flystore.persistencia.entidades.EstadoVenta;
import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CU-04 · La búsqueda de pedidos arma su HQL según los filtros presentes. MapeoJpaTest solo valida
 * las consultas base; aquí se valida, sin base de datos, cada combinación de filtros.
 */
class BusquedaPedidosDAOTest {
    private static final OffsetDateTime DIA = OffsetDateTime.parse("2026-11-20T00:00:00-07:00");

    @Test void soloAgregaLosFiltrosPresentes() {
        var sinFiltros = new CriteriosBusquedaPedidos(null, null, null, null, null, 1);
        var hql = PedidoVentaDAO.consultaBusqueda(false, sinFiltros, null);
        assertTrue(hql.endsWith("where p.estadoVenta <> :pendiente order by p.fechaHoraPedido desc, p.id desc"), hql);
        assertFalse(PedidoVentaDAO.consultaBusqueda(true, sinFiltros, null).contains("order by"));

        var todos = new CriteriosBusquedaPedidos("FLY-0001", EstadoVenta.PAGADO, CriteriosBusquedaPedidos.EFECTIVO, DIA, DIA.plusDays(1), 2);
        var porFolio = PedidoVentaDAO.consultaBusqueda(false, todos, null);
        assertTrue(porFolio.contains("upper(p.folio) like :folio") && porFolio.contains("type(pg) = PagoEfectivo")
                && porFolio.contains(":estado") && porFolio.contains(":desde") && porFolio.contains(":hasta"), porFolio);
        assertFalse(porFolio.contains(":clientes"), "Sin clientes que coincidan, el texto solo busca en el folio");
        var porClientes = PedidoVentaDAO.consultaBusqueda(false, todos, List.of(7L));
        assertTrue(porClientes.contains("(upper(p.folio) like :folio or p.cliente.id in :clientes)"), porClientes);
        assertFalse(PedidoVentaDAO.consultaBusqueda(false, sinFiltros, List.of(7L)).contains(":clientes"), "Sin texto no se filtra por cliente");
    }

    @Test void todasLasCombinacionesSonHqlValido() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.temp.use_jdbc_metadata_defaults", false)
                .applySetting("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
                .build();
        try {
            var sources = new MetadataSources(registry);
            try (var paths = Files.list(Path.of("src/main/java/com/flystore/persistencia/entidades"))) {
                for (var file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var type = Class.forName("com.flystore.persistencia.entidades." + file.getFileName().toString().replace(".java", ""));
                    if (type.isAnnotationPresent(Entity.class)) sources.addAnnotatedClass(type);
                }
            }
            var metadata = (MetadataImplementor) sources.buildMetadata();
            try (var factory = (SessionFactoryImplementor) metadata.buildSessionFactory()) {
                var translator = factory.getQueryEngine().getHqlTranslator();
                var criterios = List.of(
                        new CriteriosBusquedaPedidos(null, null, null, null, null, 1),
                        new CriteriosBusquedaPedidos("FLY-0001", EstadoVenta.PAGADO, CriteriosBusquedaPedidos.EFECTIVO, DIA, DIA.plusDays(1), 1),
                        new CriteriosBusquedaPedidos(null, null, CriteriosBusquedaPedidos.TRANSFERENCIA, null, null, 3));
                for (var c : criterios) {
                    for (var clientes : new java.util.ArrayList<List<Long>>(java.util.Arrays.asList(null, List.of(7L, 9L)))) {
                        translator.translate(PedidoVentaDAO.consultaBusqueda(true, c, clientes), null);
                        translator.translate(PedidoVentaDAO.consultaBusqueda(false, c, clientes), null);
                    }
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
