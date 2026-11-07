package CU_iniciarSesion_DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * DTO record para respuesta de autenticación (login/registro)
 */
public record AuthResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("token_type")
        String tokenType,

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
}
