package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import java.util.List;
import java.util.UUID;

public interface ICatalogoBO {

    // ===== CU-03 Administrar catálogo =====

    FiltrosDisponiblesDTO obtenerFiltrosDisponibles();

    List<TallaDTO> obtenerTallasPorCategoria(String idCategoria);

    // ===== CU-01 Consultar catálogo (tienda) =====

    /**
     * Página de productos visibles de una línea (caps, essence, clothes, shoes) con las opciones para
     * llenar sus filtros. Visibles = PUBLICADO o STOCK_BAJO, con existencia y con precio.
     *
     * @param linea   segmento de la ruta; se compara con el nombre de la categoría sin importar mayúsculas.
     * @param authId  usuario de la sesión o null; define el precio de referencia (tipo de cliente).
     */
    CatalogoRespuestaDTO consultarLinea(String linea, FiltrosCatalogoPeticionDTO filtros, UUID authId);

    /**
     * Búsqueda en toda la tienda (S1). {@code linea} opcional acota a una línea; la respuesta trae cuántos
     * resultados hay en cada línea. El texto es obligatorio (A1).
     */
    CatalogoRespuestaDTO buscar(String linea, FiltrosCatalogoPeticionDTO filtros, UUID authId);

    /** Ficha de un producto visible con sus variantes (el id de variante es el que usa «Agregar al carrito»). */
    ProductoDetalleRespuestaDTO obtenerProducto(String idProducto, UUID authId);
}
