package com.flystore.config;

import com.flystore.util.CookieUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro que extrae el JWT desde cookies HttpOnly y lo inyecta en el header Authorization
 * De esta forma, OAuth2ResourceServer puede validarlo sin cambios
 *
 * SOLID: SRP - Solo responsable de extraer JWT de cookies
 * GRASP: Indirection - Actúa como intermediario entre cookies y Spring Security
 * GRASP: Protected Variations - Protege a Spring Security de cambios en ubicación del token
 */
@Component
public class CookieJwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CookieJwtFilter.class);

    private final CookieUtils cookieUtils;

    public CookieJwtFilter(CookieUtils cookieUtils) {
        this.cookieUtils = cookieUtils;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Intentar extraer JWT desde cookie
        Optional<String> jwtFromCookie = cookieUtils.getAccessTokenFromCookies(request);

        if (jwtFromCookie.isPresent()) {
            logger.debug("JWT encontrado en cookie, inyectándolo en header Authorization");

            // Crear wrapper que intercepta getHeader("Authorization")
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    // Si Spring Security pide el header Authorization, devolver el JWT de la cookie
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return "Bearer " + jwtFromCookie.get();
                    }
                    return super.getHeader(name);
                }
            };

            filterChain.doFilter(wrappedRequest, response);
        } else {
            // No hay cookie, continuar normalmente (puede haber header Authorization manual)
            logger.debug("No se encontró JWT en cookie, continuando sin modificar request");
            filterChain.doFilter(request, response);
        }
    }

    /**
     * No aplicar este filtro a rutas públicas que no necesitan autenticación
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // No filtrar endpoints de autenticación inicial (login, registro, etc.)
        // Estos endpoints no necesitan JWT en el request (lo generan/reciben)
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/registro") ||
               path.startsWith("/api/auth/google/initiate") ||
               path.startsWith("/api/auth/google/callback") ||
               path.startsWith("/api/auth/email/callback");
    }
}
