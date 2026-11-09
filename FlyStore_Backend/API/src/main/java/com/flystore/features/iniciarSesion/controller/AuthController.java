package com.flystore.features.iniciarSesion.controller;

import CU_iniciarSesion_DTOs.AuthResponse;
import CU_iniciarSesion_DTOs.LoginRequest;
import CU_iniciarSesion_DTOs.RegisterRequest;
import CU_iniciarSesion_DTOs.RegistroPendienteResponse;
import CU_iniciarSesion_DTOs.TipoClienteResponse;
import CU_iniciarSesion_DTOs.UserInfoResponse;
import com.flystore.features.iniciarSesion.BO.IAuthBO;
import com.flystore.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller para caso de uso: Iniciar Sesión SOLID: SRP - Solo maneja HTTP (entrada/salida, códigos de respuesta) GRASP: Controller - Coordina el caso de uso de autenticación SOLID: DIP - Depende de IAuthBO (abstracción), no de implementación
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final IAuthBO authBO;
    private final CookieUtils cookieUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // SOLID: Inyección por CONSTRUCTOR (no @Autowired en campos)
    public AuthController(IAuthBO authBO, CookieUtils cookieUtils) {
        this.authBO = authBO;
        this.cookieUtils = cookieUtils;
    }

    /**
     * POST /api/auth/login Inicia sesión de usuario El access_token y refresh_token se devuelven en cookies HttpOnly
     */
    @PostMapping("/login")
    public ResponseEntity<UserInfoResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        logger.info("POST /api/auth/login - email: {}", request.email());

        AuthResponse authResponse = authBO.login(request);

        // Setear tokens en cookies HttpOnly
        cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

        // Nota: Supabase NO devuelve refresh_token en login con password
        // Solo lo devuelve en signup o en refresh. Por seguridad, lo guardamos si existe
        // En tu caso, como usas Supabase, el refresh está manejado internamente
        // Devolver solo información del usuario (sin tokens)
        return ResponseEntity.ok(UserInfoResponse.fromAuthResponse(authResponse));
    }

    /**
     * POST /api/auth/registro
     * Registra un nuevo usuario
     * - Si Supabase requiere confirmación de email: Devuelve 200 con mensaje de verificación pendiente
     * - Si NO requiere confirmación: Devuelve 201 con sesión establecida (cookies HttpOnly)
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registro(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        logger.info("POST /api/auth/registro - email: {}", request.email());

        AuthResponse authResponse = authBO.registrar(request);

        // Si authResponse es null, significa que se envió email de verificación
        if (authResponse == null) {
            logger.info("Registro requiere verificación de email: {}", request.email());
            return ResponseEntity.ok(RegistroPendienteResponse.crear(request.email()));
        }

        // Si hay authResponse, setear cookies y devolver sesión
        cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserInfoResponse.fromAuthResponse(authResponse));
    }

    /**
     * POST /api/auth/refresh Refresca el access token usando el refresh token de la cookie Nota: Actualmente Supabase no implementa refresh en login con password Este endpoint está preparado para cuando se agregue esa funcionalidad
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        logger.info("POST /api/auth/refresh");

        // Intentar obtener refresh token de cookie
        String refreshToken = cookieUtils.getRefreshTokenFromCookies(request)
                .orElseThrow(() -> new RuntimeException("No refresh token found"));

        AuthResponse authResponse = authBO.refrescarToken(refreshToken);

        // Setear nuevo access token en cookie
        cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

        // 204 No Content (el token está en la cookie)
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/auth/logout Cierra la sesión del usuario y borra las cookies
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        logger.info("POST /api/auth/logout");

        // Intentar obtener access token de cookie para invalidarlo en Supabase
        cookieUtils.getAccessTokenFromCookies(request)
                .ifPresent(accessToken -> {
                    try {
                        authBO.cerrarSesion(accessToken);
                    } catch (Exception e) {
                        logger.warn("Error al cerrar sesión en Supabase (se ignorará): {}", e.getMessage());
                    }
                });

        // Borrar cookies (independientemente de si se pudo invalidar en Supabase)
        cookieUtils.clearAllAuthCookies(response);

        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/auth/yo
     * Obtiene información del usuario autenticado actual
     * Lee el access_token de la cookie HttpOnly y valida la sesión
     *
     * Útil para que el frontend:
     * - Verifique si hay sesión activa al cargar la app
     * - Obtenga datos del usuario sin hacer login nuevamente
     * - Valide que la sesión sigue siendo válida
     *
     * @param request HTTP request que contiene la cookie con el access_token
     * @return Información del usuario autenticado
     */
    @GetMapping("/yo")
    public ResponseEntity<UserInfoResponse> obtenerUsuarioActual(HttpServletRequest request) {
        logger.info("GET /api/auth/yo - Obteniendo usuario actual");

        // Obtener access token de la cookie HttpOnly
        String accessToken = cookieUtils.getAccessTokenFromCookies(request)
                .orElseThrow(() -> new RuntimeException("No se encontró sesión activa"));

        // Validar token y obtener información del usuario
        AuthResponse authResponse = authBO.obtenerUsuarioActual(accessToken);

        // Devolver solo información del usuario (sin tokens)
        return ResponseEntity.ok(UserInfoResponse.fromAuthResponse(authResponse));
    }

    /**
     * GET /api/auth/tipos-cliente Obtiene los tipos de cliente disponibles Endpoint público para que el frontend pueda mostrar opciones en el formulario de registro
     */
    @GetMapping("/tipos-cliente")
    public ResponseEntity<List<TipoClienteResponse>> obtenerTiposCliente() {
        logger.info("GET /api/auth/tipos-cliente - Obteniendo tipos de cliente");

        List<String> tiposCliente = authBO.obtenerTiposCliente();

        List<TipoClienteResponse> response = tiposCliente.stream()
                .map(TipoClienteResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/callback
     * Endpoint de callback para verificación de email desde Supabase
     * Recibe tokens del callback, crea el cliente en PostgreSQL, establece cookies y redirige al frontend
     */
    @GetMapping("/callback")
    public void callback(
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletResponse response) throws Exception {

        logger.info("GET /api/auth/callback - Procesando callback de verificación de email");

        // Verificar si hay error en el callback
        if (error != null) {
            logger.error("Error en callback de Supabase: {} - {}", error, errorDescription);
            response.sendRedirect(frontendUrl + "?error=" + error);
            return;
        }

        // Verificar que vengan los tokens
        if (accessToken == null || accessToken.isBlank()) {
            logger.error("Callback sin access_token");
            response.sendRedirect(frontendUrl + "?error=missing_token");
            return;
        }

        try {
            // Procesar callback y crear cliente en PostgreSQL
            AuthResponse authResponse = authBO.procesarCallbackVerificacion(accessToken, refreshToken);

            // Establecer cookie HttpOnly con access_token
            cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

            logger.info("Callback procesado exitosamente - Redirigiendo a frontend: {}", frontendUrl);

            // Redirigir al frontend con sesión establecida
            response.sendRedirect(frontendUrl);

        } catch (Exception e) {
            logger.error("Error al procesar callback: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "?error=callback_failed");
        }
    }

}
