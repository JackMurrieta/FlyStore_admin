package com.flystore.security;

import com.flystore.config.SupabaseProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de autenticación para validar tokens JWT de Supabase
 * Extrae el token del header Authorization, lo valida y establece el contexto de seguridad
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    @Autowired
    private DatabaseContextManager contextManager;

    @Autowired
    private SupabaseProperties supabaseProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            try {
                // Validar y extraer claims del token JWT
                Claims claims = validateToken(token);
                String userIdStr = claims.getSubject(); // UUID del usuario
                UUID userId = UUID.fromString(userIdStr);
                String role = claims.get("role", String.class);
                String email = claims.get("email", String.class);

                // Establecer contexto de base de datos para RLS
                contextManager.setUserContext(userId, role, claims);

                // Establecer contexto de Spring Security
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                logger.debug("Usuario autenticado: " + userId + " con role: " + role);

            } catch (Exception e) {
                logger.error("Token JWT inválido: " + e.getMessage());
                // No establecer autenticación si el token es inválido
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpiar contexto de base de datos después de la petición
            if (token != null) {
                try {
                    contextManager.clearUserContext();
                } catch (Exception e) {
                    logger.error("Error al limpiar contexto de base de datos", e);
                }
            }
        }
    }

    /**
     * Extrae el token JWT del header Authorization
     * Espera formato: "Bearer {token}"
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Valida el token JWT usando el secret de Supabase
     * y extrae los claims
     */
    private Claims validateToken(String token) {
        String secret = supabaseProperties.getJwt().getSecret();

        if (secret == null || secret.isEmpty() || secret.equals("your-jwt-secret-here")) {
            throw new IllegalStateException(
                "Supabase JWT secret no está configurado. " +
                "Configura la variable de entorno SUPABASE_JWT_SECRET o " +
                "actualiza application.properties"
            );
        }

        SecretKey key = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );

        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * No filtrar rutas públicas
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Agregar aquí rutas que no requieren autenticación
        return path.startsWith("/api/public/") ||
               path.equals("/health") ||
               path.startsWith("/actuator/");
    }
}
