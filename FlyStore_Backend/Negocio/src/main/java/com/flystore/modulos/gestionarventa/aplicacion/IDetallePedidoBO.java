package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.persistencia.entidades.CodigoDescuento;
import com.flystore.persistencia.entidades.DetallePedido;
import com.flystore.persistencia.entidades.PedidoVenta;
import com.flystore.persistencia.entidades.VarianteProducto;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Reglas sobre las líneas (DetallePedido) del pedido en curso. Opera sobre el pedido ya cargado. */
public interface IDetallePedidoBO {

    /** Suma piezas a la línea de la variante o crea la línea si no existe. Devuelve las piezas resultantes. */
    int agregarVariante(PedidoVenta pedido, VarianteProducto variante, int cantidad);

    /** Cambia las piezas de la línea de la variante; con 0 quita la línea. */
    void actualizarCantidad(PedidoVenta pedido, Long varianteProductoId, int cantidad);

    /** Línea que contiene la variante, si está en el pedido. */
    Optional<DetallePedido> buscarPorVariante(PedidoVenta pedido, Long varianteProductoId);

    /**
     * Recalcula precio unitario y esquema de todas las líneas, agrupando las piezas por producto,
     * y ajusta el descuento por pieza del código aplicado.
     */
    void recalcularPrecios(PedidoVenta pedido);

    /**
     * Aplica el código a las líneas de los productos indicados; sustituye otro código que tuvieran.
     * Devuelve cuántas líneas lo recibieron.
     */
    int aplicarCodigoDescuento(PedidoVenta pedido, CodigoDescuento codigo, Set<Long> productosAplicables);

    /** Quita el código de todas las líneas que lo tengan. */
    void quitarCodigoDescuento(PedidoVenta pedido, CodigoDescuento codigo);

    /** Códigos distintos aplicados en el pedido. */
    Collection<CodigoDescuento> obtenerCodigosAplicados(PedidoVenta pedido);

    /** (precioUnitario − descuentoUnitario) × cantidad. */
    BigDecimal calcularSubtotal(DetallePedido detalle);
}
