package com.flystore.persistencia.dao;

import com.flystore.persistencia.entidades.Producto;
import jakarta.persistence.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaginacionProductoDAOTest {
    @Test void limitaLaConsultaRaizAntesDeCargarColeccionesEnTresConsultasAcotadas() throws Exception {
        comprobar(false);
        comprobar(true);
    }
    private void comprobar(boolean vacio) throws Exception {
        var consultas = new ArrayList<String>();
        var limites = new ArrayList<Integer>();
        var offsets = new ArrayList<Integer>();
        var producto = new Producto();
        var id = Producto.class.getDeclaredField("id"); id.setAccessible(true); id.set(producto, 15L);
        List<Producto> pagina = vacio ? List.of() : List.of(producto);
        var em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{EntityManager.class}, (proxy,m,a) -> {
            if (!m.getName().equals("createQuery")) throw new AssertionError(m.getName());
            String hql = (String)a[0]; consultas.add(hql);
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{TypedQuery.class}, (q,met,args) -> {
                return switch(met.getName()) {
                    case "setFirstResult" -> { assertTrue(hql.contains("join fetch p.subcategoria")); offsets.add((Integer)args[0]); yield q; }
                    case "setMaxResults" -> { limites.add((Integer)args[0]); yield q; }
                    case "setParameter" -> { if (args[0].equals("ids")) assertEquals(List.of(15L), args[1]); yield q; }
                    case "getResultList" -> pagina;
                    default -> throw new AssertionError(met.getName());
                };
            });
        });
        var dao = new ProductoDAO();
        var campo = ProductoDAO.class.getDeclaredField("entityManager"); campo.setAccessible(true); campo.set(dao,em);
        assertEquals(pagina,dao.buscarPagina(null,null,null,null,null,2,7));
        assertEquals(List.of(14),offsets);
        assertEquals(List.of(8),limites);
        assertEquals(vacio ? 1 : 4,consultas.size());
        assertTrue(consultas.getFirst().endsWith("order by p.id"));
    }
}
