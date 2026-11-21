package com.flystore.modulos.autenticacion.infraestructura.supabase;

import com.flystore.modulos.autenticacion.aplicacion.modelo.SesionAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;

import com.flystore.modulos.autenticacion.aplicacion.puertos.IProveedorAutenticacion;

import com.flystore.modulos.autenticacion.infraestructura.supabase.dto.SupabaseSession;
import com.flystore.modulos.autenticacion.infraestructura.supabase.dto.SupabaseUser;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación del gateway de Supabase Auth usando RestClient SOLID: SRP -
 * Solo responsable de comunicación con Supabase Auth API GRASP: Indirection -
 * Actúa como intermediario entre el sistema y Supabase
 */
@Component
public class SupabaseAuthGateway implements IProveedorAutenticacion {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthGateway.class);

    private final RestClient restClient;
    private final String supabaseUrl;

    public SupabaseAuthGateway(RestClient supabaseRestClient,
            com.flystore.infraestructura.config.SupabaseProperties supabaseProperties) {
        this.restClient = supabaseRestClient;
        this.supabaseUrl = supabaseProperties.getUrl();
    }

    @Override
    public SesionAutenticada registrar(String email, String password, Map<String, Object> metadata) {
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
            return normalizar(sesion);

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
    public SesionAutenticada login(String email, String password) {
        logger.debug("Iniciando sesión en Supabase: {}", email);

        try {
            SupabaseSession sesion = restClient.post()
                    .uri("/auth/v1/token?grant_type=password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(SupabaseSession.class);

            logger.info("Login exitoso: {}", email);
            return normalizar(sesion);

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
    public SesionAutenticada refrescar(String refreshToken) {
        logger.debug("Refrescando token");

        try {
            SupabaseSession sesion = restClient.post()
                    .uri("/auth/v1/token?grant_type=refresh_token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refresh_token", refreshToken))
                    .retrieve()
                    .body(SupabaseSession.class);

            logger.info("Token refrescado exitosamente");
            return normalizar(sesion);

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

    @Override
    public IdentidadAutenticada obtenerUsuario(String accessToken) {
        logger.debug("Obteniendo información del usuario desde Supabase");

        try {
            SupabaseUser usuario = restClient.get()
                    .uri("/auth/v1/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(SupabaseUser.class);

            logger.info("Usuario obtenido exitosamente");
            return normalizar(usuario);

        } catch (HttpClientErrorException e) {
            logger.error("Error 4xx al obtener usuario: {}", e.getMessage());
            throw mapearErrorCliente(e);
        } catch (HttpServerErrorException e) {
            logger.error("Error 5xx de Supabase: {}", e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Servicio de autenticación no disponible");
        }
    }

    // ===== Cuenta: correo, contraseña, enlaces de correo y Google =====

    /** Tipos de enlace que se aceptan al verificar un token_hash. */
    private static final Set<String> TIPOS_TOKEN = Set.of("email", "signup", "recovery", "email_change", "invite", "magiclink");

    @Override
    public void solicitarCambioCorreo(String accessToken, String nuevoCorreo) {
        ejecutar("solicitar cambio de correo", () -> restClient.put()
                .uri("/auth/v1/user")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", nuevoCorreo))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void actualizarContrasena(String accessToken, String nuevaContrasena) {
        ejecutar("actualizar contraseña", () -> restClient.put()
                .uri("/auth/v1/user")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("password", nuevaContrasena))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void enviarRecuperacion(String correo) {
        ejecutar("enviar recuperación", () -> restClient.post()
                .uri("/auth/v1/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", correo))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public SesionAutenticada verificarToken(String tokenHash, String tipo) {
        if (!TIPOS_TOKEN.contains(tipo)) throw new NegocioException(TipoError.DATOS_INVALIDOS, "Enlace no válido");
        return ejecutar("verificar enlace", () -> normalizarPermisivo(restClient.post()
                .uri("/auth/v1/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", tipo, "token_hash", tokenHash))
                .retrieve()
                .body(SupabaseSession.class)));
    }

    @Override
    public void cerrarSesionGlobal(String accessToken) {
        ejecutar("cerrar sesión en todos los dispositivos", () -> restClient.post()
                .uri("/auth/v1/logout?scope=global")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public String urlAutorizacion(String proveedor, String redirigirA, String codeChallenge) {
        return supabaseUrl + "/auth/v1/authorize?provider=" + codificar(proveedor)
                + "&redirect_to=" + codificar(redirigirA)
                + "&code_challenge=" + codificar(codeChallenge)
                + "&code_challenge_method=s256";
    }

    @Override
    public SesionAutenticada canjearCodigo(String codigo, String codeVerifier) {
        return ejecutar("canjear código de autorización", () -> normalizar(restClient.post()
                .uri("/auth/v1/token?grant_type=pkce")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("auth_code", codigo, "code_verifier", codeVerifier))
                .retrieve()
                .body(SupabaseSession.class)));
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    /** El cambio de correo seguro puede responder sin sesión mientras falta la otra confirmación. */
    private SesionAutenticada normalizarPermisivo(SupabaseSession sesion) {
        if (sesion == null || sesion.accessToken() == null) return new SesionAutenticada(null, null, null, null, null);
        return normalizar(sesion);
    }

    private void ejecutar(String operacion, Runnable llamada) {
        ejecutar(operacion, () -> { llamada.run(); return null; });
    }

    private <T> T ejecutar(String operacion, java.util.function.Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException e) {
            logger.warn("Error 4xx al {}: {}", operacion, e.getMessage());
            throw mapearErrorCliente(e);
        } catch (HttpServerErrorException e) {
            logger.error("Error 5xx de Supabase al {}: {}", operacion, e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Servicio de autenticación no disponible");
        } catch (ResourceAccessException e) {
            logger.error("Sin conexión con Supabase al {}: {}", operacion, e.getMessage());
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "No se pudo conectar con el servicio de autenticación");
        }
    }

    /**
     * Mapea errores 4xx de Supabase a NegocioException con TipoError apropiado
     */
    private IdentidadAutenticada normalizar(SupabaseUser user) {
        if (user == null || user.id() == null) {
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Respuesta de identidad incompleta");
        }
        return new IdentidadAutenticada(user.id(), user.email(), user.userMetadata(), vacioANull(user.newEmail()),
                proveedores(user.appMetadata()));
    }

    private static List<String> proveedores(Map<String, Object> appMetadata) {
        if (appMetadata == null) return List.of();
        if (appMetadata.get("providers") instanceof List<?> lista)
            return lista.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        return appMetadata.get("provider") instanceof String uno ? List.of(uno) : List.of();
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private SesionAutenticada normalizar(SupabaseSession session) {
        if (session == null) {
            throw new NegocioException(TipoError.SUPABASE_NO_DISPONIBLE, "Respuesta de sesión vacía");
        }
        return new SesionAutenticada(session.accessToken(), session.tokenType(), session.expiresIn(),
                session.refreshToken(), session.user() == null ? null : normalizar(session.user()));
    }

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
        if (body.contains("over_request_rate_limit") || body.contains("over_email_send_rate_limit")) {
            return new NegocioException(TipoError.DEMASIADAS_SOLICITUDES, "Demasiadas solicitudes, intenta más tarde");
        }
        if (body.contains("same_password")) {
            return new NegocioException(TipoError.DATOS_INVALIDOS, "La nueva contraseña debe ser distinta de la actual");
        }
        if (body.contains("weak_password")) {
            return new NegocioException(TipoError.DATOS_INVALIDOS, "La contraseña no cumple los requisitos de seguridad; usa una más larga, con letras y números");
        }
        if (body.contains("email_exists") || body.contains("email_address_not_authorized")) {
            return new NegocioException(TipoError.EMAIL_YA_REGISTRADO, "Ese correo ya está registrado en otra cuenta");
        }
        if (body.contains("email_address_invalid")) {
            return new NegocioException(TipoError.DATOS_INVALIDOS, "Revisa el correo; no es válido");
        }
        if (body.contains("otp_expired") || body.contains("otp_disabled")) {
            return new NegocioException(TipoError.TOKEN_EXPIRADO, "El enlace venció o ya se usó. Pide uno nuevo.");
        }
        if (body.contains("flow_state_not_found") || body.contains("flow_state_expired") || body.contains("bad_code_verifier")
                || body.contains("bad_oauth_callback") || body.contains("bad_oauth_state")) {
            return new NegocioException(TipoError.TOKEN_INVALIDO, "No pudimos completar el inicio de sesión. Inténtalo de nuevo.");
        }
        if (body.contains("reauthentication_needed") || body.contains("reauthentication_not_valid")) {
            return new NegocioException(TipoError.ACCESO_DENEGADO, "Por seguridad, cierra sesión, vuelve a entrar y repite el cambio.");
        }

        // Error genérico 4xx
        return new NegocioException(TipoError.DATOS_INVALIDOS, "Error en la solicitud: " + mensaje);
    }
}
