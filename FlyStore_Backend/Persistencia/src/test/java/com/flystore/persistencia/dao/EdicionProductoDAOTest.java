package com.flystore.persistencia.dao;

import com.flystore.persistencia.entidades.Producto;
import jakarta.persistence.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El GET del formulario de edición corre en una transacción de solo lectura: PostgreSQL rechaza
 * SELECT … FOR UPDATE ahí (error 25006, se veía como 500 ERROR_INTERNO). Abrir para editar no bloquea;
 * el bloqueo es de obtenerPorId, que el PUT llama antes de guardar.
 */
class EdicionProductoDAOTest {
    @Test void abrirParaEditarNoBloqueaYCargaLasColeccionesPorSeparado() throws Exception {
        var llamadas = new ArrayList<String>();
        var consultas = new ArrayList<String>();
        var producto = new Producto();
        var dao = dao(llamadas, consultas, producto);

        assertSame(producto, dao.obtenerParaEditar(9L).orElseThrow());
        assertEquals(List.of("find:2"), llamadas, "sin LockModeType");
        assertEquals(4, consultas.size());
        assertTrue(consultas.stream().anyMatch(c -> c.contains("left join fetch v.talla")), "variantes sin talla también se cargan");
    }

    @Test void obtenerPorIdSigueBloqueandoParaGuardar() throws Exception {
        var llamadas = new ArrayList<String>();
        var dao = dao(llamadas, new ArrayList<>(), new Producto());
        dao.obtenerPorId(9L);
        assertEquals(List.of("find:3:PESSIMISTIC_WRITE"), llamadas);
    }

    private static ProductoDAO dao(List<String> llamadas, List<String> consultas, Producto producto) throws Exception {
        var cargador = EdicionProductoDAOTest.class.getClassLoader();
        var em = (EntityManager) Proxy.newProxyInstance(cargador, new Class<?>[]{EntityManager.class}, (proxy, m, a) -> switch (m.getName()) {
            case "find" -> {
                assertEquals(Producto.class, a[0]);
                assertEquals(9L, a[1]);
                llamadas.add("find:" + a.length + (a.length > 2 ? ":" + a[2] : ""));
                yield producto;
            }
            case "createQuery" -> {
                consultas.add((String) a[0]);
                yield Proxy.newProxyInstance(cargador, new Class<?>[]{TypedQuery.class}, (q, met, args) -> switch (met.getName()) {
                    case "setParameter" -> q;
                    case "getResultList" -> List.of(producto);
                    default -> throw new AssertionError(met.getName());
                });
            }
            default -> throw new AssertionError(m.getName());
        });
        var dao = new ProductoDAO();
        var campo = ProductoDAO.class.getDeclaredField("entityManager");
        campo.setAccessible(true);
        campo.set(dao, em);
        return dao;
    }
}
