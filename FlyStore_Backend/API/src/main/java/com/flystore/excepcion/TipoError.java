package com.flystore.excepcion;

/**
 * Enum que define los tipos de errores de negocio
 * Cada tipo se mapea a un código HTTP específico
 */
public enum TipoError {

    // 400 Bad Request
    DATOS_INVALIDOS(400),
    VALIDACION_FALLIDA(400),

    // 401 Unauthorized
    CREDENCIALES_INVALIDAS(401),
    TOKEN_INVALIDO(401),
    TOKEN_EXPIRADO(401),

    // 403 Forbidden
    ACCESO_DENEGADO(403),
    EMAIL_NO_CONFIRMADO(403),

    // 404 Not Found
    USUARIO_NO_ENCONTRADO(404),
    RECURSO_NO_ENCONTRADO(404),

    // 409 Conflict
    USUARIO_YA_EXISTE(409),
    EMAIL_YA_REGISTRADO(409),

    // 429 Too Many Requests
    DEMASIADAS_SOLICITUDES(429),

    // 500 Internal Server Error
    ERROR_INTERNO(500),

    // 503 Service Unavailable
    SERVICIO_NO_DISPONIBLE(503),
    SUPABASE_NO_DISPONIBLE(503);

    private final int codigoHttp;

    TipoError(int codigoHttp) {
        this.codigoHttp = codigoHttp;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }
}
