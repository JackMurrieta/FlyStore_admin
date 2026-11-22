package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.EstadoProducto;
import com.flystore.persistencia.entidades.TipoPrecio;
import com.flystore.modulos.catalogo.aplicacion.mappers.ProductoMapper;
import com.flystore.dominio.excepcion.*;
import java.util.*;
import com.flystore.persistencia.excepcion.DatosCatalogoInvalidosException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen;
import com.flystore.modulos.catalogo.aplicacion.modelo.ImagenAlmacenada;
import com.flystore.modulos.catalogo.aplicacion.puertos.IAlmacenImagenes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductoBO implements IProductoBO {
    private static final Logger logger = LoggerFactory.getLogger(ProductoBO.class);

    private final IProductoDAO productoDAO;
    private final IDetallePedidoDAO detallePedidoDAO;
    private final ProductoMapper productoMapper;
    private final IAlmacenImagenes almacenImagenes;

    public ProductoBO(IProductoDAO productoDAO, IDetallePedidoDAO detallePedidoDAO, ProductoMapper productoMapper, IAlmacenImagenes almacenImagenes) {
        this.productoDAO = productoDAO;
        this.detallePedidoDAO = detallePedidoDAO;
        this.productoMapper = productoMapper;
        this.almacenImagenes = almacenImagenes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> obtenerProductos() {
        return productoDAO.obtenerTodos().stream().map(productoMapper::aProductoCatalogoDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> buscarProductos(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado) {
        if (vacio(texto) && vacio(idCategoria) && vacio(idSubcategoria) && vacio(idMarca) && vacio(estado))
            throw invalido("Selecciona al menos un filtro para buscar.");
        EstadoProducto filtro = null;
        if (estado != null && !estado.isBlank()) {
            try {
                filtro = EstadoProducto.valueOf(estado);
            } catch (IllegalArgumentException e) {
                throw invalido("Estado de producto inválido");
            }
        }
        return productoDAO.buscar(texto, opcional(idCategoria), opcional(idSubcategoria), opcional(idMarca), filtro).stream().map(productoMapper::aProductoCatalogoDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaProductosDTO buscarPagina(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado, int pagina, int tamano) {
        if (vacio(texto) && vacio(idCategoria) && vacio(idSubcategoria) && vacio(idMarca) && vacio(estado))
            throw invalido("Selecciona al menos un filtro para buscar.");
        return consultarPagina(texto, idCategoria, idSubcategoria, idMarca, estado, pagina, tamano);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaProductosDTO consultarPagina(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 50 || (long) pagina * tamano > Integer.MAX_VALUE)
            throw invalido("Página o tamaño inválido; el tamaño debe estar entre 1 y 50");
        EstadoProducto filtro = null;
        if (!vacio(estado)) {
            try { filtro = EstadoProducto.valueOf(estado); }
            catch (IllegalArgumentException e) { throw invalido("Estado de producto inválido"); }
        }
        var encontrados = productoDAO.buscarPagina(texto, opcional(idCategoria), opcional(idSubcategoria), opcional(idMarca), filtro, pagina, tamano);
        return new PaginaProductosDTO(encontrados.stream().limit(tamano).map(productoMapper::aProductoCatalogoDTO).toList(), pagina, tamano, encontrados.size() > tamano);
    }

    @Override
    @Transactional
    public void crearProducto(CrearProductoDTO dto) {
        validarProductoCompleto(dto);
        validarProductoNoDuplicado(dto.clave());
        try {
            productoDAO.guardar(productoMapper.aProducto(dto));
        } catch (IllegalArgumentException | DatosCatalogoInvalidosException e) {
            throw invalido(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void crearProductoConImagen(CrearProductoDTO dto, ArchivoImagen archivo) {
        validarDatosProducto(dto);
        validarStock(dto.variantes());
        validarCoherenciaPrecios(dto.preciosProducto());
        validarImagen(archivo);
        validarProductoNoDuplicado(dto.clave());
        var imagen = almacenImagenes.subir(archivo);
        boolean transaccion = TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive();
        if (transaccion) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int estado) {
                    if (estado == STATUS_ROLLED_BACK) retirarImagen(imagen);
                    else if (estado != STATUS_COMMITTED) logger.warn("Resultado de transacción desconocido; conservar imagen {} para reconciliar", imagen.ruta());
                }
            });
        }
        try {
            var completo = new CrearProductoDTO(dto.clave(), dto.nombre(), dto.descripcion(), dto.idCategoria(),
                    dto.idSubcategoria(), dto.idMarca(), List.of(imagen.url()), dto.estado(), dto.variantes(), dto.preciosProducto());
            productoDAO.guardar(productoMapper.aProducto(completo));
        } catch (RuntimeException | Error e) {
            // En producción también cubre errores durante el commit mediante afterCompletion.
            if (!transaccion) retirarImagen(imagen);
            if (e instanceof IllegalArgumentException || e instanceof DatosCatalogoInvalidosException) throw invalido(e.getMessage());
            throw e;
        }
    }

    private void retirarImagen(ImagenAlmacenada imagen) {
        try { almacenImagenes.eliminar(imagen.ruta()); }
        catch (RuntimeException e) { logger.error("No se pudo retirar la imagen {} tras fallar el alta; requiere limpieza", imagen.ruta()); }
    }

    private void validarImagen(ArchivoImagen imagen) {
        if (imagen == null || imagen.contenido() == null || imagen.contenido().length == 0) throw invalido("Selecciona una imagen");
        byte[] datos = imagen.contenido();
        if (datos.length > ArchivoImagen.MAX_BYTES) throw invalido("La imagen no debe superar 5 MB");
        boolean jpg = datos.length >= 3 && (datos[0] & 255) == 255 && (datos[1] & 255) == 216 && (datos[2] & 255) == 255;
        boolean png = datos.length >= 8 && Arrays.equals(Arrays.copyOf(datos, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
        boolean webp = datos.length >= 12 && datos[0] == 'R' && datos[1] == 'I' && datos[2] == 'F' && datos[3] == 'F'
                && datos[8] == 'W' && datos[9] == 'E' && datos[10] == 'B' && datos[11] == 'P';
        if (!(jpg && "image/jpeg".equals(imagen.tipoContenido()) || png && "image/png".equals(imagen.tipoContenido())
                || webp && "image/webp".equals(imagen.tipoContenido()))) throw invalido("El archivo debe ser una imagen JPG, PNG o WEBP válida");
    }

    @Override
    @Transactional
    public ResultadoBajaDTO darDeBajaProducto(String idProducto) {
        Long id = ProductoMapper.id(idProducto);
        productoDAO.obtenerPorId(id).orElseThrow(() -> new NegocioException(TipoError.RECURSO_NO_ENCONTRADO, "Producto no encontrado"));
        boolean ventas = tieneVentas(idProducto);
        if (ventas) {
            productoDAO.cambiarEstado(id, EstadoProducto.INACTIVO);
        } else {
            productoDAO.eliminar(id);
        }
        return productoMapper.aResultadoBajaDTO(id.toString(), ventas);
    }

    private void validarProductoCompleto(CrearProductoDTO dto) {
        validarDatosProducto(dto);
        validarStock(dto.variantes());
        validarCoherenciaPrecios(dto.preciosProducto());
        validarUrls(dto.imagenes());
    }

    private void validarDatosProducto(CrearProductoDTO dto) {
        if (dto == null || vacio(dto.clave()) || vacio(dto.nombre()) || vacio(dto.descripcion())) {
            throw invalido("Clave, nombre y descripción son obligatorios");
        }
        ProductoMapper.id(dto.idCategoria());
        ProductoMapper.id(dto.idSubcategoria());
        ProductoMapper.id(dto.idMarca());
        if (!Set.of("PUBLICADO", "BORRADOR", "INACTIVO").contains(dto.estado() == null ? "" : dto.estado())) {
            throw invalido("Estado de producto inválido");
        }
    }

    private void validarUrls(List<String> imagenes) {
        if (imagenes == null || imagenes.isEmpty()) {
            throw invalido("El producto debe tener imágenes");
        }
        for (var imagen : imagenes) {
            try {
                var uri = java.net.URI.create(imagen.trim());
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw invalido("Las imágenes deben ser URLs HTTP o HTTPS válidas");
            }
        }
    }

    private void validarProductoNoDuplicado(String clave) {
        if (productoDAO.existePorClave(clave.trim())) {
            throw new NegocioException(TipoError.RECURSO_YA_EXISTE, "La clave del producto ya existe");
        }
    }

    private void validarStock(List<VarianteDTO> variantes) {
        if (variantes == null || variantes.isEmpty()) {
            throw invalido("Debe existir al menos una variante");
        }
        var combinaciones = new HashSet<List<String>>();
        for (var v : variantes) {
            if (v == null || vacio(v.talla()) || vacio(v.color()) || v.stock() == null || v.stock() < 0) {
                throw invalido("Talla, color y stock no negativo son obligatorios");
            }
            if (!combinaciones.add(List.of(v.talla().trim().toLowerCase(Locale.ROOT), v.color().trim().toLowerCase(Locale.ROOT)))) {
                throw invalido("No se permiten variantes repetidas");
            }
        }
    }

    private void validarCoherenciaPrecios(List<PrecioProductoDTO> precios) {
        if (precios == null || precios.isEmpty()) {
            throw invalido("Debe existir al menos un precio");
        }
        var tipos = new HashSet<String>();
        for (var p : precios) {
            if (p == null || p.tipoPrecio() == null || p.precio() == null || p.precio().signum() <= 0 || p.minPiezas() == null || p.minPiezas() < 1) {
                throw invalido("Precios y cantidades mínimas deben ser positivos");
            }
            try {
                TipoPrecio.valueOf(p.tipoPrecio());
            } catch (IllegalArgumentException e) {
                throw invalido("Tipo de precio inválido");
            }
            if (!tipos.add(p.tipoPrecio())) {
                throw invalido("No se permiten tipos de precio repetidos");
            }
        }
        if (!tipos.contains("BASE")) {
            throw invalido("El precio BASE es obligatorio");
        }
        var base = precios.stream().filter(p -> p.tipoPrecio().equals("BASE")).findFirst().orElseThrow();
        if (base.minPiezas() != 1) {
            throw invalido("El precio BASE debe aplicar desde una pieza");
        }
        for (var p : precios) {
            if (p.precio().compareTo(base.precio()) > 0) {
                throw invalido("Los precios por volumen no pueden superar el precio BASE");
            }
        }
    }

    private boolean tieneVentas(String idProducto) {
        return detallePedidoDAO.existenVentasDeProducto(ProductoMapper.id(idProducto));
    }

    private Long opcional(String id) {
        return id == null || id.isBlank() ? null : ProductoMapper.id(id);
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private NegocioException invalido(String mensaje) {
        return new NegocioException(TipoError.DATOS_INVALIDOS, mensaje);
    }
}
