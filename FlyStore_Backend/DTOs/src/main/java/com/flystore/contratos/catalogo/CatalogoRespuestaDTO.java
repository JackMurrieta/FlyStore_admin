/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.flystore.contratos.catalogo;

import java.util.List;

public record CatalogoRespuestaDTO(
        List<ProductoCatalogoDTO> productos,
        OpcionesFiltroRespuestaDTO opcionesFiltro
) {
    public CatalogoRespuestaDTO(List<ProductoCatalogoDTO> productos) {
        this(productos, null);
    }
}
