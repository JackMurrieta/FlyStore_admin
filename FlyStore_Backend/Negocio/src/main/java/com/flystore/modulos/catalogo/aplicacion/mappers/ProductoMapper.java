package com.flystore.modulos.catalogo.aplicacion.mappers;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.cambios.CambiosProducto;
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

    /** Alta por JSON: las imágenes llegan como URL. */
    public Producto aProducto(CrearProductoDTO dto) {
        return aProducto(dto, List.of());
    }

    /**
     * Alta con archivos: {@code urlsArchivos.get(i)} es la URL pública del archivo {@code i}.
     * Cada imagen queda ligada a su color por nombre (Persistencia lo resuelve) y en el orden de la lista.
     */
    public Producto aProducto(CrearProductoDTO dto, List<String> urlsArchivos) {
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
        for (var origen : dto.imagenes()) {
            var imagen = new ImagenProducto();
            imagen.setUrlImagen(url(origen, urlsArchivos));
            imagen.setColor(colorPorNombre(origen.color()));
            imagen.setOrden(imagenes.size());
            imagenes.add(imagen);
        }
        producto.setImagenes(imagenes);
        return producto;
    }

    /** Edición: lo que se guarda, con las existencias que ya decidió Negocio. */
    public CambiosProducto aCambiosProducto(EditarProductoDTO dto, EstadoProducto estado, Map<Long, Integer> existencias,
            List<String> urlsArchivos) {
        var nuevas = dto.variantes().stream().filter(v -> vacio(v.idVariante()))
                .map(v -> aVarianteProducto(new VarianteDTO(v.talla(), v.color(), v.stock())))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        var imagenes = dto.imagenes().stream()
                .map(i -> new CambiosProducto.ImagenCambio(vacio(i.idImagen()) ? null : id(i.idImagen()),
                        vacio(i.idImagen()) ? url(i, urlsArchivos) : null, i.color().trim()))
                .toList();
        var precios = dto.preciosProducto().stream().map(this::aPrecioProducto)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return new CambiosProducto(dto.clave().trim(), dto.nombre().trim(), dto.descripcion().trim(),
                id(dto.idSubcategoria()), id(dto.idMarca()), estado, existencias, nuevas, imagenes, precios);
    }

    /** Producto completo para el formulario de edición. */
    public ProductoEdicionDTO aProductoEdicionDTO(Producto p, Set<Long> variantesConPedidos) {
        var subcategoria = p.getSubcategoria();
        var categoria = subcategoria.getCategoria();
        var variantes = p.getVariantes().stream()
                .sorted(Comparator.comparing(VarianteProducto::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(v -> new VarianteAdminDTO(texto(v.getId()), v.getSku(),
                        v.getTalla() == null ? null : texto(v.getTalla().getId()), v.getTalla() == null ? null : v.getTalla().getEtiqueta(),
                        texto(v.getColor().getId()), v.getColor().getNombreColor(), v.getColor().getHex(),
                        v.getStock(), variantesConPedidos.contains(v.getId())))
                .toList();
        var imagenes = p.getImagenes().stream()
                .sorted(Comparator.comparing(ImagenProducto::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(i -> new ImagenAdminDTO(texto(i.getId()), i.getUrlImagen(),
                        i.getColor() == null ? null : texto(i.getColor().getId()),
                        i.getColor() == null ? null : i.getColor().getNombreColor(), i.getOrden()))
                .toList();
        var precios = p.getPrecios().stream()
                .sorted(Comparator.comparing(PrecioProducto::getMinPiezas, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::aPrecioProductoDTO).toList();
        return new ProductoEdicionDTO(texto(p.getId()), p.getClave(), p.getNombre(), p.getDescripcion(),
                texto(categoria.getId()), categoria.getNombre(), texto(subcategoria.getId()), subcategoria.getNombre(),
                texto(p.getMarca().getId()), p.getMarca().getNombre(), p.getEstadoProducto().name(),
                variantes, precios, imagenes);
    }

    private static String url(ImagenColorDTO origen, List<String> urlsArchivos) {
        return origen.archivo() != null ? urlsArchivos.get(origen.archivo()) : origen.url().trim();
    }

    private static Color colorPorNombre(String nombre) {
        var color = new Color();
        color.setNombreColor(nombre.trim());
        return color;
    }

    private static String texto(Long id) { return id == null ? null : id.toString(); }

    private static boolean vacio(String valor) { return valor == null || valor.isBlank(); }

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
