package com.flystore.modulos.catalogo.presentacion;

import com.flystore.modulos.catalogo.aplicacion.IProductoBO;
import com.flystore.contratos.catalogo.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen;
import com.flystore.dominio.excepcion.*;
import java.io.IOException;
@RestController
@RequestMapping({"/api/productos", "/api/admin/productos"})
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> crearProducto(@Valid @RequestBody CrearProductoDTO dto) { productoBO.crearProducto(dto); return ResponseEntity.status(201).build(); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> crearProductoConImagen(@RequestPart("producto") CrearProductoDTO dto,
            @RequestPart("imagen") MultipartFile imagen) throws IOException {
        // No validar imagenes como URLs aquí: Negocio valida los datos y el archivo antes de subirlo.
        if (imagen.isEmpty() || imagen.getSize() > ArchivoImagen.MAX_BYTES)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Selecciona una imagen JPG, PNG o WEBP de hasta 5 MB");
        productoBO.crearProductoConImagen(dto, new ArchivoImagen(imagen.getBytes(), imagen.getContentType()));
        return ResponseEntity.status(201).build();
    }
    @DeleteMapping("/{idProducto}") public ResponseEntity<ResultadoBajaDTO> darDeBajaProducto(@PathVariable String idProducto) { return ResponseEntity.ok(productoBO.darDeBajaProducto(idProducto)); }
}
