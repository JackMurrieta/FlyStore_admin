package com.flystore.contratos.autenticacion;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para respuesta cuando el registro requiere verificación de email
 */
public record RegistroPendienteResponse(
        @JsonProperty("mensaje")
        String mensaje,

        @JsonProperty("email")
        String email,

        @JsonProperty("verificacion_requerida")
        boolean verificacionRequerida
) {
    public static RegistroPendienteResponse crear(String email) {
        return new RegistroPendienteResponse(
                "Te enviamos un correo de verificación. Por favor, revisa tu bandeja de entrada.",
                email,
                true
        );
    }
}
