package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.persistencia.entidades.VarianteProducto;
import java.util.List;
import org.springframework.stereotype.Component;

/** Existencia de productos y variantes para la tienda. El carrito no descuenta stock (CU-02). */
@Component
public class EvaluadorStock {

    public boolean calcularDisponibilidad(List<VarianteProducto> variantes) {
        return existenciaTotal(variantes) > 0;
    }

    public boolean disponible(VarianteProducto variante) {
        return existencia(variante) > 0;
    }

    public int existencia(VarianteProducto variante) {
        return variante == null || variante.getStock() == null ? 0 : Math.max(variante.getStock(), 0);
    }

    public int existenciaTotal(List<VarianteProducto> variantes) {
        if (variantes == null) return 0;
        return variantes.stream().mapToInt(this::existencia).sum();
    }
}
