package com.flystore.contratos.catalogo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parámetros de GET /api/catalogo/{linea}. Todos son opcionales; las listas aceptan varios valores
 * (marca=1&amp;marca=2 o marca=1,2).
 *
 * @param texto          búsqueda en nombre, modelo (clave), marca, subcategoría y línea; sin importar
 *                       mayúsculas ni acentos. En blanco = sin búsqueda.
 * @param tallas         ids de talla; talla, color y unidad de medida se cumplen en la misma variante.
 * @param unidadesMedida unidad del tipo de talla (ej. «in», «cm»).
 * @param precioMin      sobre el precio de referencia del cliente.
 * @param orden          RECIENTES (por defecto), PRECIO_ASC, PRECIO_DESC, NOMBRE_ASC o MAS_VENDIDOS.
 * @param pagina         bloque de 30 productos empezando en 0.
 */
public record FiltrosCatalogoPeticionDTO(
        String texto,
        List<Long> marcas,
        List<Long> subcategorias,
        List<Long> tallas,
        List<Long> colores,
        List<String> unidadesMedida,
        BigDecimal precioMin,
        BigDecimal precioMax,
        String orden,
        Integer pagina) {

    /** Sin búsqueda ni filtros: la primera página de la línea. */
    public static FiltrosCatalogoPeticionDTO vacio() {
        return new FiltrosCatalogoPeticionDTO(null, List.of(), List.of(), List.of(), List.of(), List.of(), null, null, null, 0);
    }
}
