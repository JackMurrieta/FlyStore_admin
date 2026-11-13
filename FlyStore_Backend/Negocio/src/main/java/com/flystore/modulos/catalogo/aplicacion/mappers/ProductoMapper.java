package com.flystore.modulos.catalogo.aplicacion.mappers;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.entidades.*;
import com.flystore.dominio.excepcion.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class ProductoMapper {
    public ProductoCatalogoDTO aProductoCatalogoDTO(Producto p) {
        var imagen = p.getImagenes() == null ? "" : p.getImagenes().stream()
                .min(Comparator.comparing(ImagenProducto::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ImagenProducto::getUrlImagen).orElse("");
        return new ProductoCatalogoDTO(p.getId().toString(), p.getClave(), p.getNombre(), imagen,
                p.getSubcategoria().getCategoria().getNombre(), p.getSubcategoria().getNombre(), p.getMarca().getNombre(),
                p.getEstadoProducto().name(), p.getVariantes().stream().map(this::aVarianteDTO).toList(),
                p.getPrecios().stream().map(this::aPrecioProductoDTO).toList());
    }

    public Producto aProducto(CrearProductoDTO dto) {
        var producto = new Producto();
        producto.setClave(dto.clave().trim()); producto.setNombre(dto.nombre().trim()); producto.setDescripcion(dto.descripcion().trim());
        producto.setEstadoProducto(EstadoProducto.valueOf(dto.estado()));
        var categoria = new Categoria(); categoria.setId(id(dto.idCategoria()));
        var subcategoria = new Subcategoria(); subcategoria.setId(id(dto.idSubcategoria())); subcategoria.setCategoria(categoria);
        var marca = new Marca(); marca.setId(id(dto.idMarca()));
        producto.setSubcategoria(subcategoria); producto.setMarca(marca);
        producto.setVariantes(dto.variantes().stream().map(this::aVarianteProducto).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        producto.setPrecios(dto.preciosProducto().stream().map(this::aPrecioProducto).collect(java.util.stream.Collectors.toCollection(ArrayList::new)));
        var imagenes = new ArrayList<ImagenProducto>();
        for (var url : dto.imagenes()) { var imagen = new ImagenProducto(); imagen.setUrlImagen(url.trim()); imagen.setOrden(imagenes.size()); imagenes.add(imagen); }
        producto.setImagenes(imagenes);
        return producto;
    }

    public VarianteDTO aVarianteDTO(VarianteProducto v) {
        return new VarianteDTO(v.getTalla() == null ? "" : v.getTalla().getEtiqueta(), v.getColor().getNombreColor(), v.getStock());
    }
    public VarianteProducto aVarianteProducto(VarianteDTO dto) {
        var variante = new VarianteProducto();
        var talla = new Talla(); talla.setEtiqueta(dto.talla().trim());
        var color = new Color(); color.setNombreColor(dto.color().trim());
        variante.setTalla(talla); variante.setColor(color); variante.setStock(dto.stock()); variante.setSku(UUID.randomUUID().toString());
        return variante;
    }
    public PrecioProductoDTO aPrecioProductoDTO(PrecioProducto p) { return new PrecioProductoDTO(p.getTipoPrecio().name(), p.getPrecio(), p.getMinPiezas()); }
    public PrecioProducto aPrecioProducto(PrecioProductoDTO dto) {
        var precio = new PrecioProducto(); precio.setTipoPrecio(TipoPrecio.valueOf(dto.tipoPrecio()));
        precio.setPrecio(dto.precio()); precio.setMinPiezas(dto.minPiezas()); return precio;
    }
    public ResultadoBajaDTO aResultadoBajaDTO(String idProducto, boolean bajaLogica) { return new ResultadoBajaDTO(idProducto, bajaLogica ? "LOGICA" : "FISICA"); }
    public static Long id(String valor) {
        try { long id = Long.parseLong(valor); if (id <= 0) throw new NumberFormatException(); return id; }
        catch (NumberFormatException | NullPointerException e) { throw new NegocioException(TipoError.DATOS_INVALIDOS, "Identificador inválido"); }
    }
}
