package com.flystore.compartido.presentacion;

import com.flystore.contratos.autenticacion.ErrorDTO;
import com.flystore.contratos.autenticacion.ValidationErrorDTO;
import com.flystore.dominio.excepcion.NegocioException;
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
 * Manejador global de excepciones SOLID: SRP - Solo maneja excepciones y las
 * convierte a respuestas HTTP Convierte excepciones a ErrorDTO estandarizado
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de negocio (NegocioException) Retorna el código HTTP
     * según el TipoError
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorDTO> handleNegocioException(NegocioException ex) {
        logger.warn("Excepción de negocio: {} - {}", ex.getTipoError(), ex.getMessage());

        ErrorDTO error = new ErrorDTO(
                ex.getTipoError().name(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(switch (ex.getTipoError()) {
            case DATOS_INVALIDOS, VALIDACION_FALLIDA ->
                400;
            case CREDENCIALES_INVALIDAS, TOKEN_INVALIDO, TOKEN_EXPIRADO ->
                401;
            case ACCESO_DENEGADO, EMAIL_NO_CONFIRMADO ->
                403;
            case USUARIO_NO_ENCONTRADO, RECURSO_NO_ENCONTRADO, NO_ENCONTRADO ->
                404;
            case USUARIO_YA_EXISTE, EMAIL_YA_REGISTRADO, RECURSO_YA_EXISTE, CONFLICTO_DE_DATOS ->
                409;
            case DEMASIADAS_SOLICITUDES ->
                429;
            case SERVICIO_NO_DISPONIBLE, SUPABASE_NO_DISPONIBLE ->
                503;
            case ERROR_INTERNO ->
                500;
        })
                .body(error);
    }

    /**
     * Maneja errores de validación (@Valid en controllers)
     * Retorna 400 Bad Request con los campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorDTO> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Error de validación: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ValidationErrorDTO errorResponse = new ValidationErrorDTO(
                "VALIDACION_FALLIDA",
                "Errores de validación",
                fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Maneja cualquier otra excepción no capturada Retorna 500 Internal Server
     * Error sin exponer detalles internos
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

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNoEncontrado(jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorDTO("RECURSO_NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> handleConflicto(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(new ErrorDTO("CONFLICTO_DE_DATOS", "La operación entra en conflicto con los datos existentes"));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.multipart.MultipartException.class})
    public ResponseEntity<ErrorDTO> handleSolicitudInvalida(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDTO("DATOS_INVALIDOS", "La solicitud contiene datos inválidos"));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDTO> handleArchivoDemasiadoGrande(Exception ex) {
        return ResponseEntity.status(413).body(new ErrorDTO("IMAGEN_DEMASIADO_GRANDE", "Cada imagen debe pesar máximo 5 MB y todas juntas 50 MB"));
    }
}
