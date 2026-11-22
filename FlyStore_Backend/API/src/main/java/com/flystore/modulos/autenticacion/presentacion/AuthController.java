package com.flystore.modulos.autenticacion.presentacion;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;

import com.flystore.modulos.autenticacion.aplicacion.modelo.AuthResponse;
import com.flystore.contratos.autenticacion.LoginRequest;
import com.flystore.contratos.autenticacion.RegisterRequest;
import com.flystore.contratos.autenticacion.RegistroPendienteResponse;
import com.flystore.contratos.autenticacion.TipoClienteResponse;
import com.flystore.contratos.autenticacion.UserInfoResponse;
import com.flystore.modulos.autenticacion.aplicacion.IAuthBO;
import com.flystore.compartido.presentacion.CookieUtils;
import com.flystore.compartido.presentacion.Pkce;
import com.flystore.contratos.autenticacion.ConfirmacionCuentaResponse;
import com.flystore.contratos.autenticacion.ConfirmarCuentaRequest;
import com.flystore.contratos.autenticacion.RecuperarContrasenaRequest;
import com.flystore.contratos.perfil.MensajeDTO;
import com.flystore.modulos.autenticacion.aplicacion.modelo.ConfirmacionCuenta;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
 * Controller para caso de uso: Iniciar Sesión SOLID: SRP - Solo maneja HTTP
 * (entrada/salida, códigos de respuesta) GRASP: Controller - Coordina el caso
 * de uso de autenticación SOLID: DIP - Depende de IAuthBO (abstracción), no de
 * implementación
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final IAuthBO authBO;
    private final CookieUtils cookieUtils;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    // SOLID: Inyección por CONSTRUCTOR (no @Autowired en campos)
    public AuthController(IAuthBO authBO, CookieUtils cookieUtils) {
        this.authBO = authBO;
        this.cookieUtils = cookieUtils;
    }

    /**
     * POST /api/auth/login Inicia sesión de usuario El access_token y
     * refresh_token se devuelven en cookies HttpOnly
     */
    @PostMapping("/login")
    public ResponseEntity<UserInfoResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        logger.info("POST /api/auth/login - email: {}", request.email());

        AuthResponse authResponse = authBO.login(request);

        // Setear tokens en cookies HttpOnly
        establecerSesion(response, authResponse);

        return ResponseEntity.ok(mapearUsuario(authResponse));
    }

    /**
     * POST /api/auth/registro Registra un nuevo usuario - Si Supabase requiere
     * confirmación de email: Devuelve 200 con mensaje de verificación pendiente
     * - Si NO requiere confirmación: Devuelve 201 con sesión establecida
     * (cookies HttpOnly)
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
        establecerSesion(response, authResponse);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapearUsuario(authResponse));
    }

    /**
     * POST /api/auth/refresh Renueva y rota los tokens de sesión almacenados en
     * cookies HttpOnly
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        logger.info("POST /api/auth/refresh");

        // Intentar obtener refresh token de cookie
        String refreshToken = cookieUtils.getRefreshTokenFromCookies(request)
                .orElseThrow(() -> new NegocioException(TipoError.TOKEN_INVALIDO, "No se encontró token de renovación"));

        AuthResponse authResponse = authBO.refrescarToken(refreshToken);

        // Setear nuevo access token en cookie
        establecerSesion(response, authResponse);

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
     * GET /api/auth/yo Obtiene información del usuario autenticado actual Lee
     * el access_token de la cookie HttpOnly y valida la sesión
     *
     * Útil para que el frontend: - Verifique si hay sesión activa al cargar la
     * app - Obtenga datos del usuario sin hacer login nuevamente - Valide que
     * la sesión sigue siendo válida
     *
     * @param request HTTP request que contiene la cookie con el access_token
     * @return Información del usuario autenticado
     */
    @GetMapping("/yo")
    public ResponseEntity<UserInfoResponse> obtenerUsuarioActual(HttpServletRequest request) {
        logger.info("GET /api/auth/yo - Obteniendo usuario actual");

        // Obtener access token de la cookie HttpOnly
        String accessToken = cookieUtils.getAccessTokenFromCookies(request)
                .orElseThrow(() -> new NegocioException(TipoError.TOKEN_INVALIDO, "No se encontró sesión activa"));

        // Validar token y obtener información del usuario
        AuthResponse authResponse = authBO.obtenerUsuarioActual(accessToken);

        // Devolver solo información del usuario (sin tokens)
        return ResponseEntity.ok(mapearUsuario(authResponse));
    }

    /**
     * GET /api/auth/tipos-cliente Obtiene los tipos de cliente disponibles
     * Endpoint público para que el frontend pueda mostrar opciones en el
     * formulario de registro
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
     * GET /api/auth/callback Endpoint de callback para verificación de email
     * desde Supabase Recibe tokens del callback, crea el cliente en PostgreSQL,
     * establece cookies y redirige al frontend
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
            establecerSesion(response, authResponse);

            logger.info("Callback procesado exitosamente - Redirigiendo a frontend: {}", frontendUrl);

            // Redirigir al frontend con sesión establecida
            response.sendRedirect(frontendUrl);

        } catch (Exception e) {
            logger.error("Error al procesar callback: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "?error=callback_failed");
        }
    }

    // ===== Cuenta: recuperar contraseña, enlaces de correo, sesión global y Google =====

    /**
     * POST /api/auth/recuperar-contrasena «Olvidé mi contraseña». Responde 202 aunque el correo
     * no tenga cuenta, para no revelar quién está registrado.
     */
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<MensajeDTO> recuperarContrasena(@Valid @RequestBody RecuperarContrasenaRequest request) {
        authBO.recuperarContrasena(request.correo());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensajeDTO(
                "Si hay una cuenta con ese correo, te enviamos un enlace para restablecer tu contraseña. Revisa también la carpeta de spam."));
    }

    /**
     * POST /api/auth/confirmar Recibe el token_hash del enlace del correo (la página
     * /auth/confirmar del front lo reenvía), lo verifica con Supabase y abre la sesión en cookies.
     */
    @PostMapping("/confirmar")
    public ResponseEntity<ConfirmacionCuentaResponse> confirmar(@Valid @RequestBody ConfirmarCuentaRequest request,
                                                                HttpServletResponse response) {
        logger.info("POST /api/auth/confirmar - tipo: {}", request.tipo());
        ConfirmacionCuenta confirmacion = authBO.confirmarCuenta(request.tokenHash(), request.tipo());
        UserInfoResponse usuario = null;
        if (confirmacion.sesion() != null) {
            establecerSesion(response, confirmacion.sesion());
            usuario = mapearUsuario(confirmacion.sesion());
        }
        return ResponseEntity.ok(new ConfirmacionCuentaResponse(confirmacion.tipo(), confirmacion.mensaje(), usuario));
    }

    /** POST /api/auth/cerrar-sesion-global Cierra la sesión en todos los dispositivos y borra las cookies. */
    @PostMapping("/cerrar-sesion-global")
    public ResponseEntity<Void> cerrarSesionGlobal(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = cookieUtils.getAccessTokenFromCookies(request)
                .orElseThrow(() -> new NegocioException(TipoError.TOKEN_INVALIDO, "No se encontró sesión activa"));
        authBO.cerrarSesionGlobal(accessToken);
        cookieUtils.clearAllAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/auth/google?redirigir=/ruta Inicia sesión con Google (PKCE). Guarda el verificador en
     * una cookie HttpOnly y redirige a Supabase, que manda al usuario a Google.
     */
    @GetMapping("/google")
    public void google(@RequestParam(value = "redirigir", required = false) String redirigir,
                       HttpServletResponse response) throws Exception {
        String verificador = Pkce.generarVerificador();
        String ruta = Pkce.rutaSegura(redirigir);
        cookieUtils.setOAuthCookie(response, verificador + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(ruta.getBytes(StandardCharsets.UTF_8)));
        response.sendRedirect(authBO.iniciarSesionConProveedor("google", backendUrl + "/api/auth/google/callback",
                Pkce.reto(verificador)));
    }

    /**
     * GET /api/auth/google/callback?code=… Supabase regresa aquí después de Google. Canjea el código
     * con el verificador, crea el cliente si es nuevo, abre la sesión y regresa al frontend.
     */
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(value = "code", required = false) String codigo,
                               @RequestParam(value = "error", required = false) String error,
                               HttpServletRequest request, HttpServletResponse response) throws Exception {
        String cookie = cookieUtils.getOAuthCookie(request).orElse("");
        cookieUtils.clearOAuthCookie(response);
        int punto = cookie.indexOf('.');
        if (error != null || codigo == null || codigo.isBlank() || punto <= 0) {
            logger.warn("Callback de Google sin código o sin verificador (error: {})", error);
            response.sendRedirect(frontendUrl + "/iniciar-sesion?error=google");
            return;
        }
        String ruta;
        try {
            ruta = Pkce.rutaSegura(new String(Base64.getUrlDecoder().decode(cookie.substring(punto + 1)), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            ruta = "/";
        }
        try {
            AuthResponse sesion = authBO.completarSesionConProveedor(codigo, cookie.substring(0, punto));
            establecerSesion(response, sesion);
            response.sendRedirect(frontendUrl + "/auth/callback?redirigir=" + URLEncoder.encode(ruta, StandardCharsets.UTF_8));
        } catch (NegocioException e) {
            logger.warn("No se pudo completar el inicio de sesión con Google: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "/iniciar-sesion?error=google");
        }
    }

    private void establecerSesion(HttpServletResponse response, AuthResponse sesion) {
        cookieUtils.setAccessTokenCookie(response, sesion.accessToken(), sesion.expiresIn());
        if (sesion.refreshToken() != null && !sesion.refreshToken().isBlank()) {
            cookieUtils.setRefreshTokenCookie(response, sesion.refreshToken());
        }
    }

    private UserInfoResponse mapearUsuario(AuthResponse sesion) {
        return new UserInfoResponse(sesion.userId(), sesion.email(), sesion.nombre(), sesion.apellidos(), sesion.tipoUsuario());
    }
}
