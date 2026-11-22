package com.flystore.contratos.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña con la sesión abierta: se confirma con la contraseña actual. */
public record CambiarContrasenaPeticionDTO(
        @NotBlank(message = "Escribe tu contraseña actual") String actual,
        @NotBlank(message = "Escribe la nueva contraseña")
        @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres") String nueva) {}
