package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.DatoBancario;
import java.util.Optional;

/** Consultas de los datos bancarios del administrador. */
public interface IDatoBancarioDAO {

    /** Cuenta activa para recibir transferencias; si hay varias, la de menor id. */
    Optional<DatoBancario> obtenerDatoBancarioActivo();
}
