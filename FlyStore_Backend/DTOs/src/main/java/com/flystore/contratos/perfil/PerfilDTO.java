package com.flystore.contratos.perfil;

import java.time.LocalDate;
import java.util.List;

/**
 * Perfil del usuario con sesión (cliente, empleado o admin).
 *
 * @param tipoUsuario     cliente | empleado | admin.
 * @param tipoCliente     esquema del cliente (BASE, MAYOREO…); null para personal. Solo lectura.
 * @param proveedores     formas de entrar: "email" (correo y contraseña), "google"…
 * @param tieneContrasena true si la cuenta tiene contraseña propia; las cuentas solo de Google no.
 * @param correoPendiente correo nuevo que espera confirmación; null si no hay cambio pendiente.
 */
public record PerfilDTO(
        Long id,
        String nombre,
        String apellidos,
        String email,
        String telefono,
        String tipoUsuario,
        String tipoCliente,
        LocalDate fechaRegistro,
        List<String> proveedores,
        boolean tieneContrasena,
        String correoPendiente) {}
