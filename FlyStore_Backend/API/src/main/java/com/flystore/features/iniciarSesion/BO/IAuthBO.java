package com.flystore.features.iniciarSesion.BO;

import CU_iniciarSesion_DTOs.AuthResponse;
import CU_iniciarSesion_DTOs.LoginRequest;
import CU_iniciarSesion_DTOs.RegisterRequest;

/**
 * Interfaz para Business Object de Autenticación
 * SOLID: ISP - Interfaz específica para autenticación
 * SOLID: DIP - Controller depende de esta abstracción
 * GRASP: Controller - Define las operaciones del caso de uso
 */
public interface IAuthBO {

    /**
     * Registra un nuevo usuario
     * @param request Datos del usuario a registrar
     * @return Respuesta con tokens y datos del usuario
     */
    AuthResponse registrar(RegisterRequest request);

    /**
     * Inicia sesión de un usuario
     * @param request Credenciales del usuario
     * @return Respuesta con tokens y datos del usuario
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refresca el access token
     * @param refreshToken Refresh token
     * @return Nueva respuesta con tokens actualizados
     */
    AuthResponse refrescarToken(String refreshToken);

    /**
     * Cierra la sesión del usuario
     * @param accessToken Access token del usuario
     */
    void cerrarSesion(String accessToken);

    /**
     * Sincroniza el usuario de Google con la base de datos local
     * @param accessToken Access token del usuario de Supabase
     * @return Respuesta con datos del usuario
     */
    AuthResponse sincronizarUsuarioGoogle(String accessToken);

    /**
     * Genera la URL de OAuth de Google a través de Supabase
     * @param callbackUrl URL a la que Supabase debe redirigir después de la autenticación
     * @return URL completa para iniciar el flujo de OAuth
     */
    String getGoogleOAuthUrl(String callbackUrl);
}
