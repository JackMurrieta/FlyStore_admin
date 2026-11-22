package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.*;
import java.util.List;
import java.util.Optional;
public interface ICategoriaDAO {
    List<Categoria> obtenerTodas();

    /** Línea por su nombre sin distinguir mayúsculas ni espacios exteriores (CU-01: caps, essence…). */
    Optional<Categoria> obtenerPorNombre(String nombre);
}
