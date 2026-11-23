package com.flystore.modulos.catalogo.presentacion;

import com.flystore.modulos.catalogo.aplicacion.IProductoBO;
import com.flystore.contratos.catalogo.*;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
    @GetMapping public ResponseEntity<?> obtenerProductos(@RequestParam(required=false) Integer pagina, @RequestParam(required=false) Integer tamano) {
        return ResponseEntity.ok(pagina == null && tamano == null ? productoBO.obtenerProductos()
            : productoBO.consultarPagina(null, null, null, null, null, pagina == null ? 0 : pagina, tamano == null ? 7 : tamano));
    }
    @GetMapping("/buscar") public ResponseEntity<?> buscarProductos(
            @RequestParam(required=false) String texto, @RequestParam(required=false) String idCategoria,
            @RequestParam(required=false) String idSubcategoria, @RequestParam(required=false) String idMarca,
            @RequestParam(required=false) String estado, @RequestParam(required=false) Integer pagina, @RequestParam(required=false) Integer tamano) {
        return ResponseEntity.ok(pagina == null && tamano == null ? productoBO.buscarProductos(texto, idCategoria, idSubcategoria, idMarca, estado)
            : productoBO.buscarPagina(texto, idCategoria, idSubcategoria, idMarca, estado, pagina == null ? 0 : pagina, tamano == null ? 7 : tamano));
    }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> crearProducto(@Valid @RequestBody CrearProductoDTO dto) { productoBO.crearProducto(dto); return ResponseEntity.status(201).build(); }

    /** Alta con archivos: parte «producto» (JSON) y parte «imagenes» (varios archivos, en el orden de producto.imagenes[].archivo). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> crearProductoConImagenes(@RequestPart("producto") CrearProductoDTO dto,
            @RequestPart(name = "imagenes", required = false) List<MultipartFile> imagenes) throws IOException {
        // No se valida el DTO aquí: Negocio valida datos, colores y archivos antes de subirlos.
        productoBO.crearProductoConImagenes(dto, archivos(imagenes));
        return ResponseEntity.status(201).build();
    }

    /** CU-03 S1 · Producto para el formulario de edición. */
    @GetMapping("/{idProducto}") public ResponseEntity<ProductoEdicionDTO> obtenerProductoParaEditar(@PathVariable String idProducto) {
        return ResponseEntity.ok(productoBO.obtenerProductoParaEditar(idProducto));
    }

    /** CU-03 S1 · Editar sin imágenes nuevas (las imágenes se conservan por idImagen). */
    @PutMapping(value = "/{idProducto}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoEdicionDTO> editarProducto(@PathVariable String idProducto, @Valid @RequestBody EditarProductoDTO dto) {
        return ResponseEntity.ok(productoBO.editarProducto(idProducto, dto, List.of()));
    }

    /** CU-03 S1 · Editar con imágenes nuevas: parte «producto» (JSON) y parte «imagenes» (archivos). */
    @PutMapping(value = "/{idProducto}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoEdicionDTO> editarProductoConImagenes(@PathVariable String idProducto,
            @RequestPart("producto") EditarProductoDTO dto,
            @RequestPart(name = "imagenes", required = false) List<MultipartFile> imagenes) throws IOException {
        return ResponseEntity.ok(productoBO.editarProducto(idProducto, dto, archivos(imagenes)));
    }

    @DeleteMapping("/{idProducto}") public ResponseEntity<ResultadoBajaDTO> darDeBajaProducto(@PathVariable String idProducto) { return ResponseEntity.ok(productoBO.darDeBajaProducto(idProducto)); }

    private static List<ArchivoImagen> archivos(List<MultipartFile> imagenes) throws IOException {
        if (imagenes == null || imagenes.isEmpty()) return List.of();
        if (imagenes.size() > ArchivoImagen.MAX_POR_PETICION)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Sube máximo " + ArchivoImagen.MAX_POR_PETICION + " imágenes por vez");
        var archivos = new ArrayList<ArchivoImagen>();
        for (var imagen : imagenes) {
            if (imagen.isEmpty() || imagen.getSize() > ArchivoImagen.MAX_BYTES)
                throw new NegocioException(TipoError.DATOS_INVALIDOS, "Cada imagen debe ser JPG, PNG o WEBP de hasta 5 MB");
            archivos.add(new ArchivoImagen(imagen.getBytes(), imagen.getContentType()));
        }
        return archivos;
    }
}
