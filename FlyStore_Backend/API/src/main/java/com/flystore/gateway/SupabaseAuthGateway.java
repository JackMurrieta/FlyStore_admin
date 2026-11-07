package com.flystore.gateway;

import CU_iniciarSesion_DTOs.SupabaseSession;
import com.flystore.excepcion.NegocioException;
import com.flystore.excepcion.TipoError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * Implementación del gateway de Supabase Auth usando RestClient
 * SOLID: SRP - Solo responsable de comunicación con Supabase Auth API
 * GRASP: Indirection - Actúa como intermediario entre el sistema y Supabase
 */
@Component
public class SupabaseAuthGateway implements ISupabaseAuthGateway {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthGateway.class);

    private final RestClient restClient;

    public SupabaseAuthGateway(RestClient supabaseRestClient) {
        this.restClient = supabaseRestClient;
    }

    @Override
    public SupabaseSession registrar(String email, String password, Map<String, Object> metadata) {
        logger.debug("Registrando usuario en Supabase: {}", email);

        try {
            SupabaseSession sesion = restClient.post()
                    .uri("/auth/v1/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", email,
                            "password", password,
                            "data", metadata != null ? metadata : Map.of()
                    ))
                    .retrieve()
                    .body(SupabaseSession.class);

            logger.info("Usuario registrado exitosamente: {}", email);
            return sesion;

        } catch (HttpClientErrorException e) {
            logger.error("Error 4xx al registrar usuario: {}", e.getMessage());
            throw mapearErrorCliente(e);
        } catch (HttpServerErrorException e) {
            logger.error("Error 5xx de Supabase: {}", e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Servicio de autenticación no disponible");
        } catch (ResourceAccessException e) {
            logger.error("Timeout al conectar con Supabase: {}", e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "No se pudo conectar con el servicio de autenticación");
        }
    }

    @Override
    public SupabaseSession login(String email, String password) {
        logger.debug("Iniciando sesión en Supabase: {}", email);

        try {
            SupabaseSession sesion = restClient.post()
                    .uri("/auth/v1/token?grant_type=password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(SupabaseSession.class);

            logger.info("Login exitoso: {}", email);
            return sesion;

        } catch (HttpClientErrorException e) {
            logger.error("Error 4xx al hacer login: {}", e.getMessage());
            throw mapearErrorCliente(e);
        } catch (HttpServerErrorException e) {
            logger.error("Error 5xx de Supabase: {}", e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Servicio de autenticación no disponible");
        } catch (ResourceAccessException e) {
            logger.error("Timeout al conectar con Supabase: {}", e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "No se pudo conectar con el servicio de autenticación");
        }
    }

    @Override
    public SupabaseSession refrescar(String refreshToken) {
        logger.debug("Refrescando token");

        try {
            SupabaseSession sesion = restClient.post()
                    .uri("/auth/v1/token?grant_type=refresh_token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refresh_token", refreshToken))
                    .retrieve()
                    .body(SupabaseSession.class);

            logger.info("Token refrescado exitosamente");
            return sesion;

        } catch (HttpClientErrorException e) {
            logger.error("Error 4xx al refrescar token: {}", e.getMessage());
            throw mapearErrorCliente(e);
        } catch (HttpServerErrorException e) {
            logger.error("Error 5xx de Supabase: {}", e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Servicio de autenticación no disponible");
        }
    }

    @Override
    public void cerrarSesion(String accessToken) {
        logger.debug("Cerrando sesión");

        try {
            restClient.post()
                    .uri("/auth/v1/logout")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Sesión cerrada exitosamente");

        } catch (HttpClientErrorException e) {
            logger.warn("Error al cerrar sesión (ignorado): {}", e.getMessage());
        }
    }

    /**
     * Mapea errores 4xx de Supabase a NegocioException con TipoError apropiado
     */
    private NegocioException mapearErrorCliente(HttpClientErrorException e) {
        String mensaje = e.getMessage();
        String body = e.getResponseBodyAsString();

        // Mapear según error_code de Supabase
        if (body.contains("invalid_credentials") || body.contains("Invalid login credentials")) {
            return new NegocioException(TipoError.CREDENCIALES_INVALIDAS, "Email o contraseña incorrectos");
        }
        if (body.contains("user_already_exists") || body.contains("User already registered")) {
            return new NegocioException(TipoError.EMAIL_YA_REGISTRADO, "El email ya está registrado");
        }
        if (body.contains("email_not_confirmed")) {
            return new NegocioException(TipoError.EMAIL_NO_CONFIRMADO, "Debes confirmar tu email primero");
        }
        if (body.contains("over_request_rate_limit")) {
            return new NegocioException(TipoError.DEMASIADAS_SOLICITUDES, "Demasiadas solicitudes, intenta más tarde");
        }

        // Error genérico 4xx
        return new NegocioException(TipoError.DATOS_INVALIDOS, "Error en la solicitud: " + mensaje);
    }
}
