package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.persistencia.entidades.PrecioProducto;
import com.flystore.persistencia.entidades.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Reglas de precio de un producto en la venta. El precio se elige por la cantidad total de piezas
 * del producto en el pedido, sumando todas sus variantes (colores y tallas).
 */
public interface IProductoVentaBO {

    /** Esquema con el mayor mínimo de piezas alcanzado; si ninguno, el precio BASE. */
    PrecioProducto determinarTipoPrecio(Producto producto, int totalPiezas);

    /** Precio BASE por pieza; si el producto no tiene BASE, el esquema de menor mínimo. */
    BigDecimal obtenerPrecioBase(Producto producto);

    /** Siguiente esquema que el producto alcanzaría con más piezas, si existe. */
    Optional<PrecioProducto> obtenerSiguienteTipoPrecio(Producto producto, int totalPiezas);

    /** Esquemas del producto ordenados por mínimo de piezas. */
    List<PrecioProducto> obtenerNivelesPrecio(Producto producto);
}
