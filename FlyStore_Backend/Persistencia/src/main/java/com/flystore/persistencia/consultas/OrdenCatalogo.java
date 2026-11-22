package com.flystore.persistencia.consultas;

/** Orden de los productos en el catálogo de la tienda (CU-01 · 3.2.3 Ordenar). */
public enum OrdenCatalogo {
    /** Productos dados de alta al último primero. */
    RECIENTES,
    /** Precio de referencia de menor a mayor. */
    PRECIO_ASC,
    /** Precio de referencia de mayor a menor. */
    PRECIO_DESC,
    /** Nombre de la A a la Z. */
    NOMBRE_ASC,
    /** Piezas vendidas en pedidos con pago confirmado. */
    MAS_VENDIDOS
}
