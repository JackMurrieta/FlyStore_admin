package com.flystore.contratos.autenticacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** «Olvidé mi contraseña»: correo al que se manda el enlace para restablecerla. */
public record RecuperarContrasenaRequest(
        @NotBlank(message = "Escribe tu correo")
        @Email(message = "Revisa el correo; no es válido") String correo) {}
