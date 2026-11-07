package com.flystore.excepcion;

/**
 * Excepción personalizada para errores de lógica de negocio
 * Se lanza cuando una regla de negocio no se cumple
 */
public class NegocioException extends RuntimeException {

    private final TipoError tipoError;

    public NegocioException(TipoError tipoError, String mensaje) {
        super(mensaje);
        this.tipoError = tipoError;
    }

    public NegocioException(TipoError tipoError, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.tipoError = tipoError;
    }

    public TipoError getTipoError() {
        return tipoError;
    }
}
