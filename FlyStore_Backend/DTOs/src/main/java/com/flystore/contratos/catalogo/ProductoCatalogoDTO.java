package com.flystore.contratos.catalogo;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ProductoCatalogoDTO(String idProducto, String clave, String nombre, String img, String categoria, String subcategoria, String marca, String estado, List<VarianteDTO> variantes, List<PrecioProductoDTO> preciosProducto) {}
