package com.flystore.modulos.administrarpedidos.aplicacion.puertos;

/**
 * Aviso al cliente sobre su pedido (CU-04). La implementación vive en Infraestructura.
 * No lanza excepciones: si no pudo avisar devuelve false y el personal lo hace a mano.
 */
public interface INotificadorCliente {

    /** Envía el mensaje al teléfono del cliente. Devuelve true solo si el proveedor lo aceptó. */
    boolean notificar(String telefono, String mensaje);
}
