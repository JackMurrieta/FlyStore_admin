package com.flystore.contratos.autenticacion;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO record para solicitud de login
 */
public record LoginRequest(
        @JsonProperty("email")
        @NotBlank(message = "El email es requerido")
        @Email(message = "Debe ser un email válido")
        String email,

        @JsonProperty("password")
        @NotBlank(message = "La contraseña es requerida")
        String password
) {
}
