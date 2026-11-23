package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.persistencia.entidades.PedidoVenta;
import com.flystore.persistencia.entidades.VarianteProducto;

/**
 * Reglas de existencia. El carrito y los pedidos sin pago o en revisión no apartan piezas: el stock
 * solo se descuenta cuando el personal confirma el pago y el pedido pasa a PAGADO. Antes de pagar
 * se avisa al cliente si pide más piezas de las que hay.
 */
public interface IVarianteProductoBO {

    /** Variante de un producto a la venta (PUBLICADO o STOCK_BAJO). */
    VarianteProducto obtenerVarianteEnVenta(Long varianteProductoId);

    /** Lanza NegocioException si la cantidad pedida supera el stock actual de la variante. */
    void validarExistencia(VarianteProducto variante, int cantidad);

    /** Valida que cada línea siga a la venta y tenga existencia suficiente, sin descontarla. */
    void validarExistencia(PedidoVenta pedido);

    /**
     * Descuenta del stock las piezas de todas las líneas del pedido, con bloqueo por variante.
     * Si alguna no alcanza lanza CONFLICTO_DE_DATOS y no descuenta nada (la transacción se revierte).
     */
    void descontarExistencia(PedidoVenta pedido);
}
