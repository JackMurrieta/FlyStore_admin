package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.contratos.gestionarventa.DatosBancariosDTO;

/** Cuenta de la tienda para recibir transferencias. */
public interface IDatoBancarioBO {

    /** Datos de la cuenta activa; SERVICIO_NO_DISPONIBLE si la tienda no ha configurado ninguna. */
    DatosBancariosDTO obtenerDatosBancarios();
}
