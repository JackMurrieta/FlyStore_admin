package com.flystore.contratos.autenticacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos del enlace que Supabase envía por correo (plantilla con token_hash).
 *
 * @param tipo email (confirmar registro), recovery (restablecer contraseña) o email_change (cambio de correo).
 */
public record ConfirmarCuentaRequest(
        @NotBlank(message = "El enlace no es válido") @Size(max = 512) String tokenHash,
        @NotBlank(message = "El enlace no es válido")
        @Pattern(regexp = "email|signup|recovery|email_change|invite|magiclink", message = "El enlace no es válido") String tipo) {}
