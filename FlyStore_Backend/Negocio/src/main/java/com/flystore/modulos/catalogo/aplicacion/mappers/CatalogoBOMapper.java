/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.flystore.modulos.catalogo.aplicacion.mappers;

import com.flystore.contratos.catalogo.CatalogoItemDTO;
import com.flystore.contratos.catalogo.CatalogoRespuestaDTO;
import com.flystore.contratos.catalogo.OpcionesFiltroRespuestaDTO;
import com.flystore.contratos.catalogo.ProductoCatalogoDTO;
import com.flystore.contratos.catalogo.ProductoDetalleRespuestaDTO;
import com.flystore.modulos.catalogo.aplicacion.modelo.CriteriosFiltro;
import com.flystore.modulos.catalogo.aplicacion.modelo.ProductoDetalleCompleto;
import com.flystore.persistencia.entidades.Categoria;
import com.flystore.persistencia.entidades.Marca;
import com.flystore.persistencia.entidades.Producto;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoBOMapper {

    private final ProductoMapper productoMapper;

    public CatalogoBOMapper(ProductoMapper productoMapper) {
        this.productoMapper = productoMapper;
    }

    public CriteriosFiltro aCriteriosFiltro(String termino, String idCategoria) {
        String terminoLimpio = (termino != null && !termino.isBlank()) ? termino.trim() : null;
        Long idCat = null;
        if (idCategoria != null && !idCategoria.isBlank()) {
            try {
                idCat = Long.valueOf(idCategoria.trim());
            } catch (NumberFormatException ignored) {
                idCat = -1L;
            }
        }
        return new CriteriosFiltro(terminoLimpio, idCat);
    }

    public CatalogoRespuestaDTO aCatalogoRespuestaDTO(List<Producto> productos) {
        return aCatalogoRespuestaDTO(productos, null);
    }

    public CatalogoRespuestaDTO aCatalogoRespuestaDTO(
            List<Producto> productos,
            OpcionesFiltroRespuestaDTO opcionesFiltro) {
        List<ProductoCatalogoDTO> items = (productos == null)
                ? Collections.emptyList()
                : productos.stream()
                        .map(productoMapper::aProductoCatalogoDTO)
                        .toList();
        return new CatalogoRespuestaDTO(items, opcionesFiltro);
    }

    public OpcionesFiltroRespuestaDTO aOpcionesFiltroRespuestaDTO(
            List<Categoria> categorias,
            List<Marca> marcas) {
        List<CatalogoItemDTO> categoriasDTO = (categorias == null)
                ? Collections.emptyList()
                : categorias.stream()
                        .map(c -> new CatalogoItemDTO(c.getId().toString(), c.getNombre()))
                        .toList();

        List<CatalogoItemDTO> marcasDTO = (marcas == null)
                ? Collections.emptyList()
                : marcas.stream()
                        .map(m -> new CatalogoItemDTO(m.getId().toString(), m.getNombre()))
                        .toList();

        return new OpcionesFiltroRespuestaDTO(categoriasDTO, marcasDTO);
    }

    public ProductoDetalleRespuestaDTO aProductoDetalleRespuestaDTO(ProductoDetalleCompleto detalle) {
        Producto p = detalle.producto();
        ProductoCatalogoDTO base = productoMapper.aProductoCatalogoDTO(p);

        List<String> imagenes = (p.getImagenes() == null)
                ? Collections.emptyList()
                : p.getImagenes().stream()
                        .map(img -> img.getUrlImagen())
                        .toList();

        CatalogoItemDTO marca = (p.getMarca() != null)
                ? new CatalogoItemDTO(p.getMarca().getId().toString(), p.getMarca().getNombre())
                : null;
        CatalogoItemDTO subcategoria = (p.getSubcategoria() != null)
                ? new CatalogoItemDTO(p.getSubcategoria().getId().toString(), p.getSubcategoria().getNombre())
                : null;
        CatalogoItemDTO categoria = (p.getSubcategoria() != null && p.getSubcategoria().getCategoria() != null)
                ? new CatalogoItemDTO(
                        p.getSubcategoria().getCategoria().getId().toString(),
                        p.getSubcategoria().getCategoria().getNombre())
                : null;

        return new ProductoDetalleRespuestaDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getEstadoProducto()!= null ? p.getEstadoProducto().name() : null,
                marca,
                categoria,
                subcategoria,
                imagenes,
                base.preciosProducto(),
                base.variantes(),
                detalle.disponible()
        );
    }
}
