package com.flystore.config;

import CU_iniciarSesion_DTOs.ErrorDTO;
import com.flystore.excepcion.NegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones
 * SOLID: SRP - Solo maneja excepciones y las convierte a respuestas HTTP
 * Convierte excepciones a ErrorDTO estandarizado
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de negocio (NegocioException)
     * Retorna el código HTTP según el TipoError
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorDTO> handleNegocioException(NegocioException ex) {
        logger.warn("Excepción de negocio: {} - {}", ex.getTipoError(), ex.getMessage());

        ErrorDTO error = new ErrorDTO(
                ex.getTipoError().name(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(ex.getTipoError().getCodigoHttp())
                .body(error);
    }

    /**
     * Maneja errores de validación (@Valid en controllers)
     * Retorna 400 Bad Request con los campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Error de validación: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("error", "VALIDACION_FALLIDA");
        response.put("message", "Errores de validación");
        response.put("fields", errors);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Maneja cualquier otra excepción no capturada
     * Retorna 500 Internal Server Error sin exponer detalles internos
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(Exception ex) {
        logger.error("Error inesperado", ex);

        ErrorDTO error = new ErrorDTO(
                "ERROR_INTERNO",
                "Ha ocurrido un error interno. Por favor intenta más tarde."
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
