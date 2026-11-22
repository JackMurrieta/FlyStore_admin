package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.modulos.autenticacion.aplicacion.modelo.AuthResponse;
import com.flystore.contratos.autenticacion.LoginRequest;
import com.flystore.contratos.autenticacion.RegisterRequest;

import java.util.List;

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
     * Obtiene todos los tipos de cliente disponibles
     * @return Lista de tipos de cliente en formato String
     */
    List<String> obtenerTiposCliente();

    /**
     * Procesa el callback de verificación de email desde Supabase
     * Crea el cliente en PostgreSQL después de verificar el email
     * @param accessToken Token de acceso recibido en el callback
     * @param refreshToken Token de refresco recibido en el callback
     * @return Respuesta con tokens y datos del usuario
     */
    AuthResponse procesarCallbackVerificacion(String accessToken, String refreshToken);

    /**
     * Obtiene información del usuario actual usando el access token
     * Útil para validar sesión activa desde el frontend
     * @param accessToken Token de acceso del usuario
     * @return Respuesta con datos del usuario actual
     */
    AuthResponse obtenerUsuarioActual(String accessToken);
}
