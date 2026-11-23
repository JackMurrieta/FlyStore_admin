package com.flystore.contratos.catalogo;

/**
 * Variante en el formulario de edición. {@code conPedidos}: está en algún carrito o pedido, por lo que
 * no se puede borrar (al quitarla se queda con existencia 0).
 */
public record VarianteAdminDTO(
        String idVariante,
        String sku,
        String idTalla,
        String talla,
        String idColor,
        String color,
        String hex,
        Integer stock,
        boolean conPedidos) {}
