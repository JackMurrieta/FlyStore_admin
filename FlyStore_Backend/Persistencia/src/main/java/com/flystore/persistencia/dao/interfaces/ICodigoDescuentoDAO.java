package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.CodigoDescuento;
import java.util.List;
import java.util.Optional;

/** Consultas de CodigoDescuento. */
public interface ICodigoDescuentoDAO {

    /** Código por su texto, sin distinguir mayúsculas ni espacios exteriores. */
    Optional<CodigoDescuento> obtenerPorCodigo(String codigo);

    /** Código con bloqueo de escritura, para contar sus usos sin carreras. */
    Optional<CodigoDescuento> bloquearPorId(Long id);

    /** IDs de los productos a los que se puede aplicar el código. */
    List<Long> obtenerIdsProductosAplicables(Long codigoDescuentoId);
}
