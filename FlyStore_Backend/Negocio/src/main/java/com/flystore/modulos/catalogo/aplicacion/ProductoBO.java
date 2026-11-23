package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.EstadoProducto;
import com.flystore.persistencia.entidades.ImagenProducto;
import com.flystore.persistencia.entidades.Producto;
import com.flystore.persistencia.entidades.TipoPrecio;
import com.flystore.persistencia.entidades.VarianteProducto;
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
    public void crearProductoConImagenes(CrearProductoDTO dto, List<ArchivoImagen> archivos) {
        var nuevos = archivos == null ? List.<ArchivoImagen>of() : archivos;
        validarDatosProducto(dto);
        validarStock(dto.variantes());
        validarCoherenciaPrecios(dto.preciosProducto());
        validarOrigenImagenes(dto.imagenes(), nuevos, false);
        validarColoresDeImagenes(dto.imagenes(), coloresDe(dto.variantes().stream().map(VarianteDTO::color).toList()));
        validarProductoNoDuplicado(dto.clave());
        var subidas = subirImagenes(nuevos);
        try {
            productoDAO.guardar(productoMapper.aProducto(dto, urls(subidas)));
        } catch (RuntimeException | Error e) {
            // Con transacción, afterCompletion retira las imágenes; sin ella se retiran aquí.
            if (!enTransaccion()) subidas.forEach(this::retirarImagen);
            if (e instanceof IllegalArgumentException || e instanceof DatosCatalogoInvalidosException) throw invalido(e.getMessage());
            throw e;
        }
    }

    // ===== CU-03 S1 · Editar producto =====

    @Override
    @Transactional(readOnly = true)
    public ProductoEdicionDTO obtenerProductoParaEditar(String idProducto) {
        Long id = ProductoMapper.id(idProducto);
        var producto = productoDAO.obtenerParaEditar(id).orElseThrow(this::productoNoEncontrado);
        return productoMapper.aProductoEdicionDTO(producto, productoDAO.variantesConPedidos(id));
    }

    @Override
    @Transactional
    public ProductoEdicionDTO editarProducto(String idProducto, EditarProductoDTO dto, List<ArchivoImagen> archivos) {
        Long id = ProductoMapper.id(idProducto);
        var nuevos = archivos == null ? List.<ArchivoImagen>of() : archivos;
        // Primero lo que no necesita la base: datos, variantes, precios e imágenes de la petición.
        validarDatosEdicion(dto);
        validarVariantesEdicion(dto.variantes());
        validarCoherenciaPrecios(dto.preciosProducto());
        validarOrigenImagenes(dto.imagenes(), nuevos, true);

        productoDAO.obtenerPorId(id).orElseThrow(this::productoNoEncontrado); // bloquea el producto
        var producto = productoDAO.obtenerParaEditar(id).orElseThrow(this::productoNoEncontrado);
        var estado = EstadoProducto.valueOf(dto.estado());
        if (estado == EstadoProducto.STOCK_BAJO && producto.getEstadoProducto() != EstadoProducto.STOCK_BAJO)
            throw invalido("Elige Publicado, Borrador o Inactivo");
        if (!dto.clave().trim().equalsIgnoreCase(producto.getClave()) && productoDAO.existeOtroConClave(dto.clave(), id))
            throw new NegocioException(TipoError.RECURSO_YA_EXISTE, "La clave del producto ya existe");

        var conPedidos = productoDAO.variantesConPedidos(id);
        var existencias = existencias(producto, dto.variantes(), conPedidos);
        validarColoresDeImagenes(dto.imagenes(), coloresActivos(producto, dto.variantes()));
        var quitadas = imagenesQuitadas(producto, dto.imagenes());

        var subidas = subirImagenes(nuevos);
        try {
            productoDAO.actualizar(id, productoMapper.aCambiosProducto(dto, estado, existencias, urls(subidas)));
        } catch (RuntimeException | Error e) {
            if (!enTransaccion()) subidas.forEach(this::retirarImagen);
            if (e instanceof IllegalArgumentException || e instanceof DatosCatalogoInvalidosException) throw invalido(e.getMessage());
            throw e;
        }
        // Un archivo quitado solo se borra de Storage si ya ningún producto lo usa y lo subió el backend.
        var rutas = quitadas.stream().filter(url -> !productoDAO.imagenEnUso(url))
                .map(almacenImagenes::rutaDe).flatMap(Optional::stream).toList();
        retirarAlConfirmar(rutas);
        return productoMapper.aProductoEdicionDTO(producto, conPedidos);
    }

    private void validarDatosEdicion(EditarProductoDTO dto) {
        if (dto == null || vacio(dto.clave()) || vacio(dto.nombre()) || vacio(dto.descripcion()))
            throw invalido("Clave, nombre y descripción son obligatorios");
        ProductoMapper.id(dto.idSubcategoria());
        ProductoMapper.id(dto.idMarca());
        if (!Set.of("PUBLICADO", "BORRADOR", "INACTIVO", "STOCK_BAJO").contains(dto.estado() == null ? "" : dto.estado()))
            throw invalido("Estado de producto inválido");
    }

    /** Variantes que ya existen (por id, solo cambia la existencia) y nuevas (talla y color), sin repetir. */
    private void validarVariantesEdicion(List<VarianteEdicionDTO> variantes) {
        if (variantes == null || variantes.isEmpty()) throw invalido("Debe existir al menos una variante");
        var ids = new HashSet<Long>();
        var combinaciones = new HashSet<List<String>>();
        for (var v : variantes) {
            if (v == null || v.stock() == null || v.stock() < 0) throw invalido("La existencia debe ser un número no negativo");
            if (!vacio(v.idVariante())) {
                if (!ids.add(ProductoMapper.id(v.idVariante()))) throw invalido("No se permiten variantes repetidas");
            } else {
                if (vacio(v.talla()) || vacio(v.color())) throw invalido("Talla, color y stock no negativo son obligatorios");
                if (!combinaciones.add(List.of(normal(v.talla()), normal(v.color())))) throw invalido("No se permiten variantes repetidas");
            }
        }
    }

    /**
     * Existencia final de cada variante que se conserva. Las que se quitan y están en un carrito o pedido
     * se conservan con existencia 0 (no se pueden borrar).
     */
    private Map<Long, Integer> existencias(Producto producto, List<VarianteEdicionDTO> pedidas, Set<Long> conPedidos) {
        var actuales = new LinkedHashMap<Long, VarianteProducto>();
        producto.getVariantes().forEach(v -> actuales.put(v.getId(), v));
        var existencias = new LinkedHashMap<Long, Integer>();
        for (var v : pedidas) {
            if (vacio(v.idVariante())) continue;
            var actual = actuales.get(ProductoMapper.id(v.idVariante()));
            if (actual == null) throw invalido("La variante " + v.idVariante().trim() + " no pertenece al producto");
            if (!vacio(v.color()) && !normal(v.color()).equals(normal(actual.getColor().getNombreColor()))
                    || !vacio(v.talla()) && !normal(v.talla()).equals(normal(etiqueta(actual))))
                throw invalido("Para cambiar el color o la talla quita la variante y agrega otra");
            existencias.put(actual.getId(), v.stock());
        }
        for (var id : actuales.keySet()) {
            if (!existencias.containsKey(id) && conPedidos.contains(id)) existencias.put(id, 0);
        }
        // Una variante nueva no puede repetir color y talla de una que se queda.
        for (var v : pedidas) {
            if (!vacio(v.idVariante())) continue;
            boolean repetida = existencias.keySet().stream().map(actuales::get)
                    .anyMatch(a -> normal(etiqueta(a)).equals(normal(v.talla())) && normal(a.getColor().getNombreColor()).equals(normal(v.color())));
            if (repetida) throw invalido("La variante " + v.color().trim() + " / " + v.talla().trim() + " ya existe");
        }
        return existencias;
    }

    /** Colores de las variantes que quedan a la venta (las que se conservan por pedidos no cuentan). */
    private Map<String, String> coloresActivos(Producto producto, List<VarianteEdicionDTO> pedidas) {
        var porId = new HashMap<Long, String>();
        producto.getVariantes().forEach(v -> porId.put(v.getId(), v.getColor().getNombreColor()));
        return coloresDe(pedidas.stream().map(v -> vacio(v.idVariante()) ? v.color() : porId.get(ProductoMapper.id(v.idVariante()))).toList());
    }

    private List<String> imagenesQuitadas(Producto producto, List<ImagenColorDTO> pedidas) {
        var conservadas = new HashSet<Long>();
        for (var i : pedidas) if (!vacio(i.idImagen())) conservadas.add(ProductoMapper.id(i.idImagen()));
        var propias = producto.getImagenes().stream().map(ImagenProducto::getId).collect(java.util.stream.Collectors.toSet());
        for (var id : conservadas) if (!propias.contains(id)) throw invalido("La imagen " + id + " no pertenece al producto");
        return producto.getImagenes().stream().filter(i -> !conservadas.contains(i.getId()))
                .map(ImagenProducto::getUrlImagen).filter(Objects::nonNull).distinct().toList();
    }

    // ===== Imágenes ligadas a color =====

    /**
     * Cada imagen dice su color y un solo origen: archivo (posición en la parte «imagenes»), url o,
     * al editar, idImagen. Todos los archivos deben estar en la lista y usarse una sola vez.
     */
    private void validarOrigenImagenes(List<ImagenColorDTO> imagenes, List<ArchivoImagen> archivos, boolean edicion) {
        if (imagenes == null || imagenes.isEmpty()) throw invalido("El producto debe tener imágenes");
        if (archivos.size() > ArchivoImagen.MAX_POR_PETICION)
            throw invalido("Sube máximo " + ArchivoImagen.MAX_POR_PETICION + " imágenes por vez");
        var usados = new HashSet<Integer>();
        var guardadas = new HashSet<Long>();
        for (var imagen : imagenes) {
            if (imagen == null || vacio(imagen.color())) throw invalido("Cada imagen debe indicar el color al que pertenece");
            int origenes = (vacio(imagen.idImagen()) ? 0 : 1) + (vacio(imagen.url()) ? 0 : 1) + (imagen.archivo() == null ? 0 : 1);
            if (origenes != 1) throw invalido("Cada imagen debe indicar un solo origen: archivo, url o idImagen");
            if (!vacio(imagen.idImagen())) {
                if (!edicion) throw invalido("Un producto nuevo no tiene imágenes guardadas: usa archivo o url");
                if (!guardadas.add(ProductoMapper.id(imagen.idImagen()))) throw invalido("No se permiten imágenes repetidas");
            } else if (!vacio(imagen.url())) {
                validarUrls(List.of(imagen.url()));
            } else if (imagen.archivo() < 0 || imagen.archivo() >= archivos.size() || !usados.add(imagen.archivo())) {
                throw invalido("Cada archivo de imagen debe aparecer una sola vez en la lista de imágenes");
            }
        }
        if (usados.size() != archivos.size()) throw invalido("Hay archivos de imagen que no están en la lista de imágenes");
        archivos.forEach(this::validarImagen);
    }

    /** Las imágenes son de colores de las variantes y cada color tiene al menos una. */
    private void validarColoresDeImagenes(List<ImagenColorDTO> imagenes, Map<String, String> colores) {
        var conImagen = new HashSet<String>();
        for (var imagen : imagenes) {
            var color = normal(imagen.color());
            if (!colores.containsKey(color))
                throw invalido("La imagen de color «" + imagen.color().trim() + "» no corresponde a ninguna variante");
            conImagen.add(color);
        }
        for (var color : colores.entrySet()) {
            if (!conImagen.contains(color.getKey())) throw invalido("Agrega al menos una imagen del color «" + color.getValue() + "»");
        }
    }

    /** Colores sin repetir: nombre normalizado → como se escribió. */
    private Map<String, String> coloresDe(List<String> nombres) {
        var colores = new LinkedHashMap<String, String>();
        for (var nombre : nombres) if (!vacio(nombre)) colores.putIfAbsent(normal(nombre), nombre.trim());
        return colores;
    }

    /** Sube los archivos; si falla una subida o la transacción se revierte, retira los ya subidos. */
    private List<ImagenAlmacenada> subirImagenes(List<ArchivoImagen> archivos) {
        var subidas = new ArrayList<ImagenAlmacenada>();
        try {
            for (var archivo : archivos) subidas.add(almacenImagenes.subir(archivo));
        } catch (RuntimeException e) {
            subidas.forEach(this::retirarImagen);
            throw e;
        }
        if (!subidas.isEmpty() && enTransaccion()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int estado) {
                    if (estado == STATUS_ROLLED_BACK) subidas.forEach(ProductoBO.this::retirarImagen);
                    else if (estado != STATUS_COMMITTED)
                        subidas.forEach(i -> logger.warn("Resultado de transacción desconocido; conservar imagen {} para reconciliar", i.ruta()));
                }
            });
        }
        return subidas;
    }

    /** Borra de Storage cuando la transacción se confirma (si se revierte, las imágenes se quedan). */
    private void retirarAlConfirmar(List<String> rutas) {
        if (rutas.isEmpty()) return;
        if (!enTransaccion()) { rutas.forEach(this::retirarRuta); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { rutas.forEach(ProductoBO.this::retirarRuta); }
        });
    }

    private static boolean enTransaccion() {
        return TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive();
    }

    private static List<String> urls(List<ImagenAlmacenada> subidas) {
        return subidas.stream().map(ImagenAlmacenada::url).toList();
    }

    private void retirarImagen(ImagenAlmacenada imagen) {
        retirarRuta(imagen.ruta());
    }

    private void retirarRuta(String ruta) {
        try { almacenImagenes.eliminar(ruta); }
        catch (RuntimeException e) { logger.error("No se pudo retirar la imagen {} de Storage; requiere limpieza", ruta); }
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
        validarOrigenImagenes(dto.imagenes(), List.of(), false);
        validarColoresDeImagenes(dto.imagenes(), coloresDe(dto.variantes().stream().map(VarianteDTO::color).toList()));
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

    private static String normal(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private static String etiqueta(VarianteProducto variante) {
        return variante.getTalla() == null ? "" : variante.getTalla().getEtiqueta();
    }

    private NegocioException productoNoEncontrado() {
        return new NegocioException(TipoError.RECURSO_NO_ENCONTRADO, "Producto no encontrado");
    }

    private NegocioException invalido(String mensaje) {
        return new NegocioException(TipoError.DATOS_INVALIDOS, mensaje);
    }
}
