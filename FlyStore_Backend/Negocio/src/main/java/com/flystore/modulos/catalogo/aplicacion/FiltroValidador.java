/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.modulos.catalogo.aplicacion.modelo.CriteriosFiltro;
import org.springframework.stereotype.Component;

@Component
public class FiltroValidador {

    public boolean validarCriterios(CriteriosFiltro criterios) {
        if (criterios == null) {
            return false;
        }
        boolean tieneTermino = criterios.termino() != null && !criterios.termino().isBlank();
        boolean tieneCategoria = criterios.idCategoria() != null && criterios.idCategoria() > 0;
        return tieneTermino || tieneCategoria;
    }
}
