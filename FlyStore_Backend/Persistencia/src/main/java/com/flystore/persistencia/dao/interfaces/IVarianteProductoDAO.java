package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.VarianteProducto;
import java.util.Optional;

/** Consultas de VarianteProducto para la venta. */
public interface IVarianteProductoDAO {

    /** Variante con su producto. Optional vacío si no existe. */
    Optional<VarianteProducto> obtenerPorId(Long id);

    /** Variante con bloqueo de escritura, para descontar existencia sin carreras. */
    Optional<VarianteProducto> bloquearPorId(Long id);
}
