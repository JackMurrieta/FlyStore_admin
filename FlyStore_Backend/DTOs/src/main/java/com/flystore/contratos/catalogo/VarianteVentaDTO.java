package com.flystore.contratos.catalogo;

/**
 * Variante que el cliente puede elegir en la ficha. {@code idVariante} es el
 * {@code varianteProductoId} que se manda a POST /api/ventas/pedido-en-curso/detalles.
 *
 * @param disponible false si no tiene existencia (A5: «Agotado», sin «Agregar al carrito»).
 */
public record VarianteVentaDTO(
        Long idVariante,
        String sku,
        Long idColor,
        String color,
        String hex,
        Long idTalla,
        String talla,
        Long idTipoTalla,
        String tipoTalla,
        String unidadMedida,
        int existencia,
        boolean disponible) {}
