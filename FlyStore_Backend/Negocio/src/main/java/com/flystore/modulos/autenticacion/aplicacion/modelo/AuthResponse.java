package com.flystore.modulos.autenticacion.aplicacion.modelo;



import java.util.UUID;

/**
 * DTO record para respuesta de autenticación (login/registro)
 */
public record AuthResponse(
        String accessToken,
        Long expiresIn,
        String tokenType,
        UUID userId,
        String email,
        String nombre,
        String apellidos,
        String tipoUsuario,
        String refreshToken
) {
}
