package com.flystore.contratos.autenticacion;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO record para respuestas de error de validación
 * Extiende ErrorDTO con campos adicionales para errores de validación
 * Usado en respuestas 400 Bad Request cuando @Valid falla
 */
public record ValidationErrorDTO(
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("fields") Map<String, String> fields,
        @JsonProperty("timestamp") long timestamp
) {
    /**
     * Constructor simplificado que genera timestamp automáticamente
     */
    public ValidationErrorDTO(String error, String message, Map<String, String> fields) {
        this(error, message, fields, System.currentTimeMillis());
    }
}
