package com.flystore.modulos.catalogo.presentacion;

import com.flystore.modulos.catalogo.aplicacion.ICatalogoBO;
import com.flystore.contratos.catalogo.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/catalogo")
public class CatalogoController {
    private final ICatalogoBO catalogoBO;
    public CatalogoController(ICatalogoBO catalogoBO) { this.catalogoBO=catalogoBO; }
    @GetMapping("/filtros") public ResponseEntity<FiltrosDisponiblesDTO> obtenerFiltrosDisponibles() { return ResponseEntity.ok(catalogoBO.obtenerFiltrosDisponibles()); }
    @GetMapping("/categorias/{idCategoria}/tallas") public ResponseEntity<List<TallaDTO>> obtenerTallasPorCategoria(@PathVariable String idCategoria) { return ResponseEntity.ok(catalogoBO.obtenerTallasPorCategoria(idCategoria)); }
}
