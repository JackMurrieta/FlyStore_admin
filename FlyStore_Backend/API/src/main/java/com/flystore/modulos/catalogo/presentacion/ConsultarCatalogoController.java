package com.flystore.modulos.catalogo.presentacion;

import com.flystore.compartido.presentacion.SesionOpcional;
import com.flystore.contratos.catalogo.CatalogoRespuestaDTO;
import com.flystore.contratos.catalogo.FiltrosCatalogoPeticionDTO;
import com.flystore.contratos.catalogo.ProductoDetalleRespuestaDTO;
import com.flystore.modulos.catalogo.aplicacion.ICatalogoBO;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CU-01 Consultar catálogo. Rutas públicas (sin sesión); si hay sesión de cliente, los precios de
 * referencia son los de su tipo de cliente. Agregar al carrito es CU-02:
 * POST /api/ventas/pedido-en-curso/detalles con el idVariante de la ficha.
 */
@RestController
@RequestMapping("/api/catalogo")
public class ConsultarCatalogoController {

    private final ICatalogoBO catalogoBO;
    private final SesionOpcional sesion;

    public ConsultarCatalogoController(ICatalogoBO catalogoBO, SesionOpcional sesion) {
        this.catalogoBO = catalogoBO;
        this.sesion = sesion;
    }

    /**
     * Productos de una línea (caps, essence, clothes, shoes) en bloques de 30, con búsqueda, filtros,
     * orden y las opciones para llenar los filtros. Las rutas literales del catálogo admin
     * (/filtros) tienen prioridad sobre este patrón.
     */
    @GetMapping("/{linea}")
    public ResponseEntity<CatalogoRespuestaDTO> consultarLinea(
            @PathVariable String linea,
            @RequestParam(required = false) String texto,
            @RequestParam(name = "marca", required = false) List<Long> marcas,
            @RequestParam(name = "subcategoria", required = false) List<Long> subcategorias,
            @RequestParam(name = "talla", required = false) List<Long> tallas,
            @RequestParam(name = "color", required = false) List<Long> colores,
            @RequestParam(name = "unidadMedida", required = false) List<String> unidadesMedida,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") Integer pagina,
            HttpServletRequest request) {
        var filtros = new FiltrosCatalogoPeticionDTO(texto, marcas, subcategorias, tallas, colores, unidadesMedida,
                precioMin, precioMax, orden, pagina);
        return ResponseEntity.ok(catalogoBO.consultarLinea(linea, filtros, sesion.usuario(request).orElse(null)));
    }

    /** 3.2.4 · Ficha del producto con variantes (color, talla, existencia) y esquemas de precio. */
    @GetMapping("/productos/{idProducto}")
    public ResponseEntity<ProductoDetalleRespuestaDTO> obtenerProducto(@PathVariable String idProducto, HttpServletRequest request) {
        return ResponseEntity.ok(catalogoBO.obtenerProducto(idProducto, sesion.usuario(request).orElse(null)));
    }
}
