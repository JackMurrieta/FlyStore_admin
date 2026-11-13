package com.flystore.contratos.catalogo;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record FiltrosDisponiblesDTO(List<CatalogoItemDTO> categorias, List<CatalogoItemDTO> subcategorias, List<CatalogoItemDTO> marcas) {}
