package CU_iniciarSesion_DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO record para respuestas de error estandarizadas
 * Usado en respuestas 4xx y 5xx
 */
public record ErrorDTO(
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") long timestamp
) {
    public ErrorDTO(String error, String message) {
        this(error, message, System.currentTimeMillis());
    }
}
