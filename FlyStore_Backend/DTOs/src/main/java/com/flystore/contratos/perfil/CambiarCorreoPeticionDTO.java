package com.flystore.contratos.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarCorreoPeticionDTO(
        @NotBlank(message = "Escribe tu nuevo correo")
        @Email(message = "Revisa el correo; no es válido")
        @Size(max = 255) String correo) {}
