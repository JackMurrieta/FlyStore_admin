/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.flystore.modulos.catalogo.aplicacion.modelo;

import com.flystore.persistencia.entidades.Producto;

public record ProductoDetalleCompleto(
        Producto producto,
        boolean disponible
        ) {

}
