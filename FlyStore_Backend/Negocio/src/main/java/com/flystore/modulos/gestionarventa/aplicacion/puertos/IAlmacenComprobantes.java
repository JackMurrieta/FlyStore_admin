package com.flystore.modulos.gestionarventa.aplicacion.puertos;

/**
 * Almacenamiento privado de comprobantes de pago. La implementación vive en Infraestructura.
 * Los archivos no son públicos: el administrador los consulta desde el backend.
 */
public interface IAlmacenComprobantes {

    /** Guarda el archivo en la ruta indicada y devuelve esa misma ruta. */
    String subir(String ruta, byte[] contenido, String tipoContenido);

    /** Elimina un comprobante guardado. */
    void eliminar(String ruta);

    /** Enlace firmado que caduca a los segundos indicados, para que el personal vea el comprobante. */
    String generarUrlTemporal(String ruta, int segundos);
}
