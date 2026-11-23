package com.flystore.modulos.gestionarventa.aplicacion.modelo;

/** Envío capturado al marcar un pedido como ENVIADO (CU-04): paquetería y guía, o entrega local. */
public record DatosEnvio(String paqueteria, String guia, boolean entregaLocal) {

    public DatosEnvio {
        paqueteria = paqueteria == null || paqueteria.isBlank() ? null : paqueteria.trim();
        guia = guia == null || guia.isBlank() ? null : guia.trim();
    }

    /** Entrega local, o paquetería y guía capturadas. */
    public boolean completo() {
        return entregaLocal || (paqueteria != null && guia != null);
    }
}
