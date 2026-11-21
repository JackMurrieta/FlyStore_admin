package com.flystore.modulos.autenticacion.aplicacion.modelo;

import java.util.List;
import java.util.Map;

/**
 * Identidad externa normalizada; no expone el contrato HTTP del proveedor.
 *
 * @param nuevoCorreo correo que espera confirmación tras pedir un cambio; null si no hay cambio pendiente.
 * @param proveedores formas de entrar de la cuenta: "email" (correo y contraseña), "google"…
 */
public record IdentidadAutenticada(String id, String email, Map<String, Object> userMetadata,
                                   String nuevoCorreo, List<String> proveedores) {
    public IdentidadAutenticada {
        userMetadata = userMetadata == null ? Map.of() : Map.copyOf(userMetadata);
        proveedores = proveedores == null ? List.of() : List.copyOf(proveedores);
    }

    /** Identidad sin cambio de correo pendiente ni proveedores conocidos. */
    public IdentidadAutenticada(String id, String email, Map<String, Object> userMetadata) {
        this(id, email, userMetadata, null, List.of());
    }

    /** La cuenta tiene contraseña propia (se registró con correo), no solo Google. */
    public boolean tieneContrasena() {
        return proveedores.contains("email");
    }
}
