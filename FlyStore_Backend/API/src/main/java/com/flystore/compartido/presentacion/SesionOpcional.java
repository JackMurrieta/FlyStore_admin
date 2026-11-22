package com.flystore.compartido.presentacion;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Sesión opcional para rutas públicas: si llega un access token válido (cookie o cabecera) devuelve
 * el usuario; si no hay token o ya venció, la petición sigue como visitante en lugar de responder 401.
 * Lo usa el catálogo de la tienda para mostrar el precio del tipo de cliente.
 */
@Component
public class SesionOpcional {

    private final CookieUtils cookieUtils;
    private final JwtDecoder jwtDecoder;

    public SesionOpcional(CookieUtils cookieUtils, JwtDecoder jwtDecoder) {
        this.cookieUtils = cookieUtils;
        this.jwtDecoder = jwtDecoder;
    }

    public Optional<UUID> usuario(HttpServletRequest request) {
        var token = cookieUtils.getAccessTokenFromCookies(request).or(() -> bearer(request));
        if (token.isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(jwtDecoder.decode(token.get()).getSubject()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> bearer(HttpServletRequest request) {
        var cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) return Optional.empty();
        var token = cabecera.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
