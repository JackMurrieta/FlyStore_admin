/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.flystore.modulos.catalogo.presentacion;

import com.flystore.contratos.catalogo.CatalogoRespuestaDTO;
import com.flystore.contratos.catalogo.ProductoDetalleRespuestaDTO;
import com.flystore.modulos.catalogo.aplicacion.ICatalogoBO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogo")
public class ConsultarCatalogoController {

    private final ICatalogoBO catalogoBO;

    public ConsultarCatalogoController(ICatalogoBO catalogoBO) {
        this.catalogoBO = catalogoBO;
    }

    @GetMapping("/inicial")
    public ResponseEntity<CatalogoRespuestaDTO> obtenerCatalogoInicial() {
        return ResponseEntity.ok(catalogoBO.obtenerCatalogoInicial());
    }

    @GetMapping("/buscar")
    public ResponseEntity<CatalogoRespuestaDTO> buscarProductos(
            @RequestParam(required = false) String termino,
            @RequestParam(required = false) String idCategoria) {
        return ResponseEntity.ok(catalogoBO.buscarProductos(termino, idCategoria));
    }

    @GetMapping("/producto/{id}")
    public ResponseEntity<ProductoDetalleRespuestaDTO> obtenerDetalleProducto(
            @PathVariable("id") Long idProducto) {
        try {
            return ResponseEntity.ok(catalogoBO.obtenerDetalleProducto(idProducto));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
