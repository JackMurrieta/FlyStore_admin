package CU_iniciarSesion_DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO para respuesta de autenticación SIN token
 * El token viaja en cookie HttpOnly, no en el body JSON
 */
public record UserInfoResponse(
        @JsonProperty("user_id")
        UUID userId,

        @JsonProperty("email")
        String email,

        @JsonProperty("nombre")
        String nombre,

        @JsonProperty("apellidos")
        String apellidos,

        @JsonProperty("tipo_usuario")
        String tipoUsuario
) {
    /**
     * Crea UserInfoResponse desde AuthResponse
     */
    public static UserInfoResponse fromAuthResponse(AuthResponse authResponse) {
        return new UserInfoResponse(
                authResponse.userId(),
                authResponse.email(),
                authResponse.nombre(),
                authResponse.apellidos(),
                authResponse.tipoUsuario()
        );
    }
}
