package com.flystore.arquitectura;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ComposicionProyectosTest {
    @Test
    void springDescubreLosComponentesEnLosProyectosDeBiblioteca() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(true);
        var componentes = scanner.findCandidateComponents("com.flystore").stream()
                .map(bean -> bean.getBeanClassName()).collect(Collectors.toSet());
        var esperados = Map.ofEntries(
                Map.entry("com.flystore.modulos.catalogo.aplicacion.ProductoBO", "Negocio"),
                Map.entry("com.flystore.modulos.catalogo.aplicacion.CatalogoBO", "Negocio"),
                Map.entry("com.flystore.persistencia.dao.ProductoDAO", "Persistencia"),
                Map.entry("com.flystore.modulos.catalogo.presentacion.ProductoController", "API"),
                Map.entry("com.flystore.modulos.catalogo.presentacion.CatalogoController", "API"),
                Map.entry("com.flystore.modulos.autenticacion.aplicacion.AuthBO", "Negocio"),
                Map.entry("com.flystore.persistencia.conexion.ConexionBD", "Persistencia"),
                Map.entry("com.flystore.persistencia.dao.ClienteDAO", "Persistencia"),
                Map.entry("com.flystore.modulos.autenticacion.infraestructura.supabase.SupabaseAuthGateway", "Infraestructura"),
                Map.entry("com.flystore.infraestructura.config.RestClientConfig", "Infraestructura"),
                Map.entry("com.flystore.modulos.autenticacion.presentacion.AuthController", "API"),
                Map.entry("com.flystore.presentacion.seguridad.SecurityConfig", "API"));
        for (var entrada : esperados.entrySet()) {
            assertTrue(componentes.contains(entrada.getKey()), entrada.getKey());
            var origen = Class.forName(entrada.getKey()).getProtectionDomain().getCodeSource()
                    .getLocation().toURI().getPath().replace('\\', '/');
            assertTrue(origen.contains("/" + entrada.getValue() + "/target/"), origen);
        }
    }
}
