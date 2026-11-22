package com.flystore.contratos.catalogo;
import java.util.List;
/** Página acotada; hayMas evita consultar un COUNT de todo el catálogo. */
public record PaginaProductosDTO(List<ProductoCatalogoDTO> productos, int pagina, int tamano, boolean hayMas) {}
