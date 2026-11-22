/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.persistencia.entidades.VarianteProducto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EvaluadorStock {

    public boolean calcularDisponibilidad(List<VarianteProducto> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            return false;
        }
        return variantes.stream()
                .anyMatch(v -> v.getStock() != null && v.getStock() > 0);
    }
}
