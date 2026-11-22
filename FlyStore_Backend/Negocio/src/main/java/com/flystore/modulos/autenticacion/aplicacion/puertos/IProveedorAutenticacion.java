package com.flystore.modulos.autenticacion.aplicacion.puertos;

import com.flystore.modulos.autenticacion.aplicacion.modelo.SesionAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;

import java.util.Map;

/**
 * Interfaz para gateway de Supabase Auth
 * GRASP: Protected Variations - Aísla al sistema de cambios en Supabase
 * SOLID: DIP - Dependemos de abstracción, no de implementación concreta
 */
public interface IProveedorAutenticacion {

    /**
     * Registra un nuevo usuario en Supabase Auth
     * @param email Email del usuario
     * @param password Contraseña
     * @param metadata Metadata adicional (nombre, apellidos, etc.)
     * @return Sesión de Supabase con tokens
     */
    SesionAutenticada registrar(String email, String password, Map<String, Object> metadata);

    /**
     * Inicia sesión con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña
     * @return Sesión de Supabase con tokens
     */
    SesionAutenticada login(String email, String password);

    /**
     * Refresca el access token usando el refresh token
     * @param refreshToken Refresh token
     * @return Nueva sesión con tokens actualizados
     */
    SesionAutenticada refrescar(String refreshToken);

    /**
     * Cierra la sesión del usuario
     * @param accessToken Access token del usuario
     */
    void cerrarSesion(String accessToken);

    /**
     * Obtiene información del usuario usando un access token
     * @param accessToken Access token del usuario
     * @return Usuario de Supabase con información del usuario
     */
    IdentidadAutenticada obtenerUsuario(String accessToken);

    /**
     * Pide cambiar el correo de la cuenta. El proveedor envía un enlace de confirmación;
     * el correo cambia hasta que se confirma.
     */
    void solicitarCambioCorreo(String accessToken, String nuevoCorreo);

    /** Cambia la contraseña de la cuenta dueña del token (el token debe ser reciente). */
    void actualizarContrasena(String accessToken, String nuevaContrasena);

    /** Envía el correo para restablecer la contraseña. No revela si el correo existe. */
    void enviarRecuperacion(String correo);

    /**
     * Verifica el token_hash de un enlace de correo (registro, recuperación o cambio de correo).
     * @return sesión abierta por el enlace; sin accessToken si el proveedor espera otra confirmación.
     */
    SesionAutenticada verificarToken(String tokenHash, String tipo);

    /** Cierra todas las sesiones de la cuenta, en todos los dispositivos. */
    void cerrarSesionGlobal(String accessToken);

    /**
     * URL para iniciar sesión con un proveedor externo (Google) usando PKCE.
     * @param redirigirA    URL del backend que recibirá el código.
     * @param codeChallenge reto S256 del code_verifier que guarda el backend.
     */
    String urlAutorizacion(String proveedor, String redirigirA, String codeChallenge);

    /** Canjea el código de autorización (PKCE) por una sesión. */
    SesionAutenticada canjearCodigo(String codigo, String codeVerifier);
}
