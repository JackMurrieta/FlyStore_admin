package com.flystore.persistencia.excepcion;
/** Datos de catálogo que no se pueden resolver a entidades existentes. */
public class DatosCatalogoInvalidosException extends RuntimeException {
 public DatosCatalogoInvalidosException(String mensaje) { super(mensaje); }
}
