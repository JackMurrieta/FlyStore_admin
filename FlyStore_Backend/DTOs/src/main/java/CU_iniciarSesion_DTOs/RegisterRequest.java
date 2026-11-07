package CU_iniciarSesion_DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO record para solicitud de registro
 */
public record RegisterRequest(
        @JsonProperty("nombre")
        @NotBlank(message = "El nombre es requerido")
        String nombre,

        @JsonProperty("apellidos")
        @NotBlank(message = "Los apellidos son requeridos")
        String apellidos,

        @JsonProperty("email")
        @NotBlank(message = "El email es requerido")
        @Email(message = "Debe ser un email válido")
        String email,

        @JsonProperty("password")
        @NotBlank(message = "La contraseña es requerida")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @JsonProperty("telefono")
        String telefono,

        @JsonProperty("tipo_usuario")
        @NotBlank(message = "El tipo de usuario es requerido")
        String tipoUsuario
) {
}
