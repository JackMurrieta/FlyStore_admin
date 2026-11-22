package com.flystore.contratos.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nueva contraseña después de abrir el enlace «Restablecer contraseña» del correo. */
public record RestablecerContrasenaPeticionDTO(
        @NotBlank(message = "Escribe la nueva contraseña")
        @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres") String nueva) {}
