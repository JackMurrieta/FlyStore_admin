package com.flystore.dominio.excepcion;

/**
 * Enum que define los tipos de errores de negocio
 * La capa de presentación decide cómo traducirlos al protocolo HTTP.
 */
public enum TipoError {

    // 400 Bad Request
    DATOS_INVALIDOS,
    VALIDACION_FALLIDA,

    // 401 Unauthorized
    CREDENCIALES_INVALIDAS,
    TOKEN_INVALIDO,
    TOKEN_EXPIRADO,

    // 403 Forbidden
    ACCESO_DENEGADO,
    EMAIL_NO_CONFIRMADO,

    // 404 Not Found
    USUARIO_NO_ENCONTRADO,
    RECURSO_NO_ENCONTRADO,

    // 409 Conflict
    USUARIO_YA_EXISTE,
    EMAIL_YA_REGISTRADO,
    RECURSO_YA_EXISTE,

    // 429 Too Many Requests
    DEMASIADAS_SOLICITUDES,

    // 500 Internal Server Error
    ERROR_INTERNO,

    // 503 Service Unavailable
    SERVICIO_NO_DISPONIBLE,
    SUPABASE_NO_DISPONIBLE;

}
