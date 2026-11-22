package com.flystore.presentacion.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Maneja errores 401 (no autenticado) retornando JSON en lugar de HTML
 * SOLID: SRP - Solo maneja errores de autenticación
 */
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "No autenticado");
        errorResponse.put("message", "Se requiere un token válido para acceder a este recurso");
        errorResponse.put("timestamp", System.currentTimeMillis());

        response.getOutputStream().println(objectMapper.writeValueAsString(errorResponse));
    }
}
