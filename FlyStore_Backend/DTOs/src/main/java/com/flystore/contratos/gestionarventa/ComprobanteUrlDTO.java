package com.flystore.contratos.gestionarventa;

/** Enlace temporal para que el personal vea un comprobante del bucket privado. */
public record ComprobanteUrlDTO(String url, int expiraEnSegundos) {}
