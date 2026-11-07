package com.flystore.gateway;

import CU_iniciarSesion_DTOs.SupabaseSession;

import java.util.Map;

/**
 * Interfaz para gateway de Supabase Auth
 * GRASP: Protected Variations - Aísla al sistema de cambios en Supabase
 * SOLID: DIP - Dependemos de abstracción, no de implementación concreta
 */
public interface ISupabaseAuthGateway {

    /**
     * Registra un nuevo usuario en Supabase Auth
     * @param email Email del usuario
     * @param password Contraseña
     * @param metadata Metadata adicional (nombre, apellidos, etc.)
     * @return Sesión de Supabase con tokens
     */
    SupabaseSession registrar(String email, String password, Map<String, Object> metadata);

    /**
     * Inicia sesión con email y contraseña
     * @param email Email del usuario
     * @param password Contraseña
     * @return Sesión de Supabase con tokens
     */
    SupabaseSession login(String email, String password);

    /**
     * Refresca el access token usando el refresh token
     * @param refreshToken Refresh token
     * @return Nueva sesión con tokens actualizados
     */
    SupabaseSession refrescar(String refreshToken);

    /**
     * Cierra la sesión del usuario
     * @param accessToken Access token del usuario
     */
    void cerrarSesion(String accessToken);
}
