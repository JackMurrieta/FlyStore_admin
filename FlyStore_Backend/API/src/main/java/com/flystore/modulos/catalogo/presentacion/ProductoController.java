package com.flystore.modulos.catalogo.presentacion;

import com.flystore.modulos.catalogo.aplicacion.IProductoBO;
import com.flystore.contratos.catalogo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/productos")
public class ProductoController {
    private final IProductoBO productoBO;
    public ProductoController(IProductoBO productoBO) { this.productoBO=productoBO; }
    @GetMapping public ResponseEntity<List<ProductoCatalogoDTO>> obtenerProductos() { return ResponseEntity.ok(productoBO.obtenerProductos()); }
    @GetMapping("/buscar") public ResponseEntity<List<ProductoCatalogoDTO>> buscarProductos(
            @RequestParam(required=false) String texto, @RequestParam(required=false) String idCategoria,
            @RequestParam(required=false) String idSubcategoria, @RequestParam(required=false) String idMarca,
            @RequestParam(required=false) String estado) {
        return ResponseEntity.ok(productoBO.buscarProductos(texto, idCategoria, idSubcategoria, idMarca, estado));
    }
    @PostMapping public ResponseEntity<Void> crearProducto(@Valid @RequestBody CrearProductoDTO dto) { productoBO.crearProducto(dto); return ResponseEntity.status(201).build(); }
    @DeleteMapping("/{idProducto}") public ResponseEntity<ResultadoBajaDTO> darDeBajaProducto(@PathVariable String idProducto) { return ResponseEntity.ok(productoBO.darDeBajaProducto(idProducto)); }
}
