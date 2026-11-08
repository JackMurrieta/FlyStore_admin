package com.flystore.features.iniciarSesion.controller;

import CU_iniciarSesion_DTOs.AuthResponse;
import CU_iniciarSesion_DTOs.LoginRequest;
import CU_iniciarSesion_DTOs.RegisterRequest;
import CU_iniciarSesion_DTOs.UserInfoResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.io.IOException;

/**
 * Controller para caso de uso: Iniciar Sesión
 * SOLID: SRP - Solo maneja HTTP (entrada/salida, códigos de respuesta)
 * GRASP: Controller - Coordina el caso de uso de autenticación
 * SOLID: DIP - Depende de IAuthBO (abstracción), no de implementación
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final IAuthBO authBO;
    private final CookieUtils cookieUtils;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // SOLID: Inyección por CONSTRUCTOR (no @Autowired en campos)
    public AuthController(IAuthBO authBO, CookieUtils cookieUtils) {
        this.authBO = authBO;
        this.cookieUtils = cookieUtils;
    }

    /**
     * POST /api/auth/login
     * Inicia sesión de usuario
     * El access_token y refresh_token se devuelven en cookies HttpOnly
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
     * Si Supabase requiere confirmación de email, NO se devuelve sesión
     * Si NO requiere confirmación, se setean cookies HttpOnly automáticamente
     */
    @PostMapping("/registro")
    public ResponseEntity<UserInfoResponse> registro(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        logger.info("POST /api/auth/registro - email: {}", request.email());

        AuthResponse authResponse = authBO.registrar(request);

        // Setear tokens en cookies HttpOnly
        cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

        // Devolver solo información del usuario (sin tokens)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserInfoResponse.fromAuthResponse(authResponse));
    }

    /**
     * POST /api/auth/refresh
     * Refresca el access token usando el refresh token de la cookie
     * Nota: Actualmente Supabase no implementa refresh en login con password
     * Este endpoint está preparado para cuando se agregue esa funcionalidad
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
     * POST /api/auth/logout
     * Cierra la sesión del usuario y borra las cookies
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
     * GET /api/auth/google/initiate
     * Inicia el flujo de OAuth con Google
     * Redirige al usuario a la página de autorización de Google vía Supabase
     */
    @GetMapping("/google/initiate")
    public void initiateGoogleOAuth(HttpServletResponse response) throws IOException {
        logger.info("GET /api/auth/google/initiate - Iniciando flujo OAuth con Google");

        // Construir URL de OAuth de Supabase con redirect apuntando al backend
        String callbackUrl = backendUrl + "/api/auth/google/callback";
        String supabaseOAuthUrl = authBO.getGoogleOAuthUrl(callbackUrl);

        // Redirigir al usuario a Supabase OAuth
        response.sendRedirect(supabaseOAuthUrl);
    }

    /**
     * GET /api/auth/google/callback
     * Callback de Google OAuth manejado por el BACKEND
     * Supabase redirige aquí después de la autenticación con Google
     *
     * Flujo:
     * 1. Usuario autoriza en Google
     * 2. Google → Supabase → Backend (este endpoint)
     * 3. Backend extrae access_token del hash
     * 4. Backend sincroniza usuario con BD local
     * 5. Backend setea cookies HttpOnly
     * 6. Backend redirige a frontend (sin token en URL)
     */
    @GetMapping("/google/callback")
    public void handleGoogleCallback(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        logger.info("GET /api/auth/google/callback - Recibiendo callback de Google OAuth");

        try {
            // Supabase devuelve el token en el HASH (#access_token=...)
            // Pero el backend no puede leer el hash (solo lo ve el navegador)
            // Por eso necesitamos un paso intermedio con JavaScript

            // Servir una página HTML que extraiga el token del hash y lo envíe al backend
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Autenticando con Google...</title>
                </head>
                <body>
                    <p>Autenticando con Google...</p>
                    <script>
                        // Extraer access_token del hash
                        const hash = window.location.hash.substring(1);
                        const params = new URLSearchParams(hash);
                        const accessToken = params.get('access_token');

                        if (accessToken) {
                            // Enviar token al backend para sincronización
                            fetch('%s/api/auth/google/complete', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({ access_token: accessToken }),
                                credentials: 'include'  // Incluir cookies
                            })
                            .then(response => {
                                if (response.ok) {
                                    // Redirigir al frontend (ya tiene las cookies)
                                    window.location.href = '%s';
                                } else {
                                    alert('Error al autenticar con Google');
                                    window.location.href = '%s/login?error=google_auth_failed';
                                }
                            })
                            .catch(error => {
                                console.error('Error:', error);
                                alert('Error al autenticar con Google');
                                window.location.href = '%s/login?error=google_auth_failed';
                            });
                        } else {
                            alert('No se recibió token de autenticación');
                            window.location.href = '%s/login?error=no_token';
                        }
                    </script>
                </body>
                </html>
                """.formatted(backendUrl, frontendUrl, frontendUrl, frontendUrl, frontendUrl);

            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(html);

        } catch (Exception e) {
            logger.error("Error en callback de Google OAuth", e);
            response.sendRedirect(frontendUrl + "/login?error=google_auth_failed");
        }
    }

    /**
     * POST /api/auth/google/complete
     * Completa el flujo de Google OAuth
     * Recibe el access_token extraído del hash por JavaScript
     * y setea las cookies HttpOnly
     */
    @PostMapping("/google/complete")
    public ResponseEntity<UserInfoResponse> completeGoogleAuth(
            @RequestBody GoogleTokenRequest tokenRequest,
            HttpServletResponse response) {

        logger.info("POST /api/auth/google/complete - Completando autenticación con Google");

        try {
            // Sincronizar usuario con BD local usando el token de Supabase
            AuthResponse authResponse = authBO.sincronizarUsuarioGoogle(tokenRequest.accessToken());

            // Setear cookies HttpOnly
            cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

            // Devolver información del usuario
            return ResponseEntity.ok(UserInfoResponse.fromAuthResponse(authResponse));

        } catch (Exception e) {
            logger.error("Error al completar autenticación con Google", e);
            throw e;
        }
    }

    /**
     * GET /api/auth/email/callback
     * Callback de confirmación de email
     * Supabase redirige aquí después de que el usuario confirma su email
     *
     * Flujo similar a Google OAuth:
     * 1. Usuario hace clic en link de confirmación
     * 2. Supabase valida y redirige aquí con token en hash
     * 3. Backend extrae token, setea cookies y redirige a frontend
     */
    @GetMapping("/email/callback")
    public void handleEmailConfirmationCallback(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        logger.info("GET /api/auth/email/callback - Recibiendo callback de confirmación de email");

        try {
            // Similar a Google callback, necesitamos JavaScript para extraer el token del hash
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Confirmando email...</title>
                </head>
                <body>
                    <p>Confirmando tu email...</p>
                    <script>
                        const hash = window.location.hash.substring(1);
                        const params = new URLSearchParams(hash);
                        const accessToken = params.get('access_token');

                        if (accessToken) {
                            fetch('%s/api/auth/email/complete', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({ access_token: accessToken }),
                                credentials: 'include'
                            })
                            .then(response => {
                                if (response.ok) {
                                    window.location.href = '%s?email_confirmed=true';
                                } else {
                                    window.location.href = '%s/login?error=email_confirmation_failed';
                                }
                            })
                            .catch(error => {
                                console.error('Error:', error);
                                window.location.href = '%s/login?error=email_confirmation_failed';
                            });
                        } else {
                            window.location.href = '%s/login?error=no_token';
                        }
                    </script>
                </body>
                </html>
                """.formatted(backendUrl, frontendUrl, frontendUrl, frontendUrl, frontendUrl);

            response.setContentType("text/html; charset=UTF-8");
            response.getWriter().write(html);

        } catch (Exception e) {
            logger.error("Error en callback de confirmación de email", e);
            response.sendRedirect(frontendUrl + "/login?error=email_confirmation_failed");
        }
    }

    /**
     * POST /api/auth/email/complete
     * Completa el flujo de confirmación de email
     */
    @PostMapping("/email/complete")
    public ResponseEntity<UserInfoResponse> completeEmailConfirmation(
            @RequestBody GoogleTokenRequest tokenRequest,
            HttpServletResponse response) {

        logger.info("POST /api/auth/email/complete - Completando confirmación de email");

        try {
            // El token ya está validado por Supabase, solo necesitamos obtener info del usuario
            AuthResponse authResponse = authBO.sincronizarUsuarioGoogle(tokenRequest.accessToken());

            // Setear cookies HttpOnly
            cookieUtils.setAccessTokenCookie(response, authResponse.accessToken(), authResponse.expiresIn());

            // Devolver información del usuario
            return ResponseEntity.ok(UserInfoResponse.fromAuthResponse(authResponse));

        } catch (Exception e) {
            logger.error("Error al completar confirmación de email", e);
            throw e;
        }
    }

    /**
     * DTO interno para recibir el access_token desde JavaScript
     */
    private record GoogleTokenRequest(
            @JsonProperty("access_token")
            String accessToken
    ) {}
}
