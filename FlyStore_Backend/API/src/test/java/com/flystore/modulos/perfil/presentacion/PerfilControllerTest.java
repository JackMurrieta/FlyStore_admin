package com.flystore.modulos.perfil.presentacion;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** La nueva contraseña solo se acepta con una sesión abierta por el enlace de recuperación reciente. */
class PerfilControllerTest {
    private static JwtAuthenticationToken sesion(String metodo, long hace) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "ES256").subject("u")
                .claim("amr", List.of(Map.of("method", metodo, "timestamp", Instant.now().getEpochSecond() - hace)))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void recuperacionDeHacePocosMinutos() {
        assertTrue(PerfilController.recuperacionReciente(sesion("recovery", 60)));
    }

    @Test
    void recuperacionVencidaOSesionNormalNoSirven() {
        assertFalse(PerfilController.recuperacionReciente(sesion("recovery", 60 * 60)));
        assertFalse(PerfilController.recuperacionReciente(sesion("password", 10)));
        assertFalse(PerfilController.recuperacionReciente(null));
    }
}
