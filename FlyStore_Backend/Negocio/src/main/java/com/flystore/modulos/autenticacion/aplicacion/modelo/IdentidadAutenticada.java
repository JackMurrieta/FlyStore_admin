package com.flystore.modulos.autenticacion.aplicacion.modelo;
import java.util.Map;
/** Identidad externa normalizada; no expone el contrato HTTP del proveedor. */
public record IdentidadAutenticada(String id, String email, Map<String, Object> userMetadata) {
    public IdentidadAutenticada {
        userMetadata = userMetadata == null ? Map.of() : Map.copyOf(userMetadata);
    }
}
