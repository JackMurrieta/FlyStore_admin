package com.flystore.modulos.autenticacion;

import com.flystore.compartido.presentacion.*;
import com.flystore.modulos.autenticacion.aplicacion.IAuthBO;
import com.flystore.modulos.autenticacion.aplicacion.modelo.AuthResponse;
import com.flystore.modulos.autenticacion.presentacion.AuthController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static com.flystore.soporte.Dobles.puerto;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private MockMvc mvc;
    @BeforeEach
    void preparar() {
        var sesion = new AuthResponse("access", 3600L, "Bearer", UUID.randomUUID(),
                "ana@example.com", "Ana", "López", "cliente", "rotado");
        var servicio = puerto(IAuthBO.class, (m,a) -> sesion);
        var cookies = new CookieUtils();
        ReflectionTestUtils.setField(cookies, "cookieDomain", "");
        ReflectionTestUtils.setField(cookies, "cookieSecure", true);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(servicio, cookies))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void loginEntregaCookiesHttpOnlySinExponerTokensEnJson() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"ana@example.com\",\"password\":\"secreto\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("access_token", "access"))
                .andExpect(cookie().value("refresh_token", "rotado"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().secure("refresh_token", true))
                .andExpect(jsonPath("$.tipo_usuario").value("cliente"))
                .andExpect(jsonPath("$.access_token").doesNotExist())
                .andExpect(jsonPath("$.refresh_token").doesNotExist());
    }

    @Test
    void refreshRotaAmbasCookies() throws Exception {
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "anterior")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().value("access_token", "access"))
                .andExpect(cookie().value("refresh_token", "rotado"));
    }

    @Test
    void ausenciaDeSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/auth/yo")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }
}
