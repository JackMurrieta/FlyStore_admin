package com.flystore.infraestructura;

import com.flystore.compartido.presentacion.CookieUtils;
import com.flystore.presentacion.seguridad.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.List;
import java.util.Optional;
import com.flystore.persistencia.dao.interfaces.*;
import static com.flystore.soporte.Dobles.puerto;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityConfigTest {
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;

    @BeforeEach void preparar() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(SecurityConfig.class, Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void cerrar() { context.close(); }

    @Test void rutasPrivadasExigenAutenticacion() throws Exception {
        mvc.perform(get("/api/protegido")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/catalogo/filtros")).andExpect(status().isUnauthorized());
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test void administracionExigeEmpleadoOAdmin() throws Exception {
        mvc.perform(get("/api/admin/prueba").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
        for (var role : new String[]{"ROLE_ADMIN", "ROLE_EMPLEADO"}) {
            mvc.perform(get("/api/admin/prueba").with(jwt().authorities(new SimpleGrantedAuthority(role))))
                    .andExpect(status().isOk());
        }
        for (var ruta : new String[]{"/api/productos", "/api/catalogo/filtros"}) {
            mvc.perform(get(ruta).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                    .andExpect(status().isForbidden());
            for (var role : new String[]{"ROLE_ADMIN", "ROLE_EMPLEADO"}) {
                mvc.perform(get(ruta).with(jwt().authorities(new SimpleGrantedAuthority(role))))
                        .andExpect(status().isOk());
            }
        }
    }

    @Test void cookieAccessVencidaNoImpideLlegarAlCasoDeUsoRefresh() throws Exception {
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("access_token", "vencido"),
                        new Cookie("refresh_token", "vigente")))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    static class Config {
        @Bean CookieUtils cookies() { return new CookieUtils(); }
        @Bean CookieJwtFilter filter(CookieUtils cookies) { return new CookieJwtFilter(cookies); }
        @Bean SupabaseJwtConverter converter() {
            return new SupabaseJwtConverter(puerto(IClienteDAO.class, (m,a) -> Optional.empty()), puerto(IEmpleadoDAO.class, (m,a) -> Optional.empty()), puerto(IAdminDAO.class, (m,a) -> Optional.empty()));
        }
        @Bean JwtDecoder decoder() { return token -> { throw new BadJwtException("Token vencido"); }; }
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }

    @RestController
    static class Endpoints {
        @GetMapping({"/health", "/api/protegido", "/api/admin/prueba", "/api/productos", "/api/catalogo/filtros"})
        String get() { return "ok"; }
        @PostMapping("/api/auth/refresh") String refresh() { return "ok"; }
    }
}
