package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import java.util.List;

public interface ICatalogoBO {

    FiltrosDisponiblesDTO obtenerFiltrosDisponibles();

    List<TallaDTO> obtenerTallasPorCategoria(String idCategoria);

    CatalogoRespuestaDTO obtenerCatalogoInicial();

    CatalogoRespuestaDTO buscarProductos(String termino, String idCategoria);

    ProductoDetalleRespuestaDTO obtenerDetalleProducto(Long idProducto);
}
