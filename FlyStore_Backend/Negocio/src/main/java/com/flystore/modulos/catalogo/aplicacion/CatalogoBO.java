package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.catalogo.aplicacion.mappers.*;
import com.flystore.persistencia.consultas.FiltroCatalogo;
import com.flystore.persistencia.consultas.OrdenCatalogo;
import com.flystore.persistencia.consultas.PaginaProductos;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoBO implements ICatalogoBO {

    /** Productos por bloque (Figma CU-01 S3: «Cargando 30 productos más»). */
    public static final int TAMANO_PAGINA = 30;
    /** Productos sugeridos cuando la búsqueda no tiene resultados (A2). */
    public static final int MAX_SUGERENCIAS = 4;

    private final ICategoriaDAO categoriaDAO;
    private final ISubcategoriaDAO subcategoriaDAO;
    private final IMarcaDAO marcaDAO;
    private final ITallaDAO tallaDAO;
    private final IColorDAO colorDAO;
    private final IProductoDAO productoDAO;
    private final IClienteDAO clienteDAO;
    private final CatalogoMapper catalogoMapper;
    private final CatalogoBOMapper catalogoBOMapper;
    private final FiltroValidador filtroValidador;
    private final EvaluadorStock evaluadorStock;

    public CatalogoBO(ICategoriaDAO categoriaDAO, ISubcategoriaDAO subcategoriaDAO, IMarcaDAO marcaDAO, ITallaDAO tallaDAO,
            IColorDAO colorDAO, IProductoDAO productoDAO, IClienteDAO clienteDAO, CatalogoMapper catalogoMapper,
            CatalogoBOMapper catalogoBOMapper, FiltroValidador filtroValidador, EvaluadorStock evaluadorStock) {
        this.categoriaDAO = categoriaDAO;
        this.subcategoriaDAO = subcategoriaDAO;
        this.marcaDAO = marcaDAO;
        this.tallaDAO = tallaDAO;
        this.colorDAO = colorDAO;
        this.productoDAO = productoDAO;
        this.clienteDAO = clienteDAO;
        this.catalogoMapper = catalogoMapper;
        this.catalogoBOMapper = catalogoBOMapper;
        this.filtroValidador = filtroValidador;
        this.evaluadorStock = evaluadorStock;
    }

    // ===== CU-03 Administrar catálogo =====

    @Override
    public FiltrosDisponiblesDTO obtenerFiltrosDisponibles() {
        return catalogoMapper.aFiltrosDisponiblesDTO(categoriaDAO.obtenerTodas(), subcategoriaDAO.obtenerTodas(), marcaDAO.obtenerTodas(), colorDAO.obtenerTodos());
    }

    @Override
    public List<TallaDTO> obtenerTallasPorCategoria(String idCategoria) {
        return tallaDAO.obtenerPorCategoria(ProductoMapper.id(idCategoria)).stream().map(catalogoMapper::aTallaDTO).toList();
    }

    // ===== CU-01 Consultar catálogo =====

    @Override
    public CatalogoRespuestaDTO consultarLinea(String linea, FiltrosCatalogoPeticionDTO filtros, UUID authId) {
        var peticion = filtros == null ? FiltrosCatalogoPeticionDTO.vacio() : filtros;
        filtroValidador.validar(peticion);
        var categoria = obtenerLinea(linea);
        var tipoPrecio = tipoPrecioDelCliente(authId);
        int pagina = filtroValidador.validarPagina(peticion.pagina());
        var filtro = aFiltroCatalogo(categoria.getId(), peticion, tipoPrecio);

        var resultado = productoDAO.buscarEnTienda(filtro, pagina * TAMANO_PAGINA, TAMANO_PAGINA);
        var productos = tarjetas(resultado, tipoPrecio);
        var opciones = catalogoBOMapper.aOpcionesFiltroRespuestaDTO(productoDAO.obtenerOpcionesFiltro(filtro.soloLinea()));
        var sugerencias = resultado.total() == 0 && filtro.tieneTexto() && pagina == 0
                ? sugerencias(filtro, tipoPrecio) : List.<ProductoTarjetaDTO>of();
        return catalogoBOMapper.aCatalogoRespuestaDTO(categoria, productos, opciones, pagina, TAMANO_PAGINA,
                resultado.total(), tipoPrecio, sugerencias, List.of());
    }

    @Override
    public CatalogoRespuestaDTO buscar(String linea, FiltrosCatalogoPeticionDTO filtros, UUID authId) {
        var peticion = filtros == null ? FiltrosCatalogoPeticionDTO.vacio() : filtros;
        filtroValidador.validar(peticion);
        if (filtroValidador.terminos(peticion.texto()).isEmpty())
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Escribe un nombre, modelo o marca para buscar");
        var categoria = linea == null || linea.isBlank() ? null : obtenerLinea(linea);
        var tipoPrecio = tipoPrecioDelCliente(authId);
        int pagina = filtroValidador.validarPagina(peticion.pagina());
        var filtro = aFiltroCatalogo(categoria == null ? null : categoria.getId(), peticion, tipoPrecio);

        var resultado = productoDAO.buscarEnTienda(filtro, pagina * TAMANO_PAGINA, TAMANO_PAGINA);
        var productos = tarjetas(resultado, tipoPrecio);
        var opciones = catalogoBOMapper.aOpcionesFiltroRespuestaDTO(productoDAO.obtenerOpcionesFiltro(filtro.soloLinea()));
        var lineas = productoDAO.contarPorLinea(filtro.soloTexto());
        var sugerencias = resultado.total() == 0 && pagina == 0 ? sugerencias(filtro, tipoPrecio) : List.<ProductoTarjetaDTO>of();
        return catalogoBOMapper.aCatalogoRespuestaDTO(categoria, productos, opciones, pagina, TAMANO_PAGINA,
                resultado.total(), tipoPrecio, sugerencias, lineas);
    }

    @Override
    public ProductoDetalleRespuestaDTO obtenerProducto(String idProducto, UUID authId) {
        Long id = ProductoMapper.id(idProducto);
        var producto = productoDAO.obtenerVisibleEnTienda(id)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Este producto no está disponible"));
        var niveles = nivelesPrecio(producto);
        var referencia = precioReferencia(niveles, tipoPrecioDelCliente(authId))
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Este producto no está disponible"));
        return catalogoBOMapper.aProductoDetalleRespuestaDTO(producto, niveles, referencia, evaluadorStock::existencia);
    }

    // ===== Reglas del catálogo de la tienda =====

    /**
     * Precio de referencia según el tipo de cliente: cliente MAYOREO ve Mayoreo, etc. Sin sesión, o si
     * quien entra no es cliente (admin o empleado), se usa BASE. El carrito cobra por piezas (CU-02).
     */
    TipoPrecio tipoPrecioDelCliente(UUID authId) {
        if (authId == null) return TipoPrecio.BASE;
        return clienteDAO.findByAuthId(authId)
                .map(Cliente::getTipoCliente)
                .map(tipo -> {
                    try {
                        return TipoPrecio.valueOf(tipo.name());
                    } catch (IllegalArgumentException e) {
                        return TipoPrecio.BASE;
                    }
                })
                .orElse(TipoPrecio.BASE);
    }

    /** Esquemas completos del producto ordenados por mínimo de piezas y precio. */
    List<PrecioProducto> nivelesPrecio(Producto producto) {
        var precios = producto.getPrecios() == null ? List.<PrecioProducto>of() : producto.getPrecios();
        return precios.stream()
                .filter(p -> p.getTipoPrecio() != null && p.getPrecio() != null && p.getMinPiezas() != null)
                .sorted(Comparator.comparing(PrecioProducto::getMinPiezas).thenComparing(PrecioProducto::getPrecio))
                .toList();
    }

    /**
     * Esquema igual al tipo de cliente; si el producto no lo tiene, BASE; si tampoco, el de menos piezas.
     * Es la misma regla con la que el DAO filtra y ordena por precio.
     */
    Optional<PrecioProducto> precioReferencia(List<PrecioProducto> niveles, TipoPrecio tipo) {
        return niveles.stream().filter(p -> p.getTipoPrecio() == tipo).findFirst()
                .or(() -> niveles.stream().filter(p -> p.getTipoPrecio() == TipoPrecio.BASE).findFirst())
                .or(() -> niveles.stream().findFirst());
    }

    private Categoria obtenerLinea(String linea) {
        var nombre = linea == null ? "" : linea.trim().replace('-', ' ');
        return categoriaDAO.obtenerPorNombre(nombre)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "No existe la línea «" + linea + "»"));
    }

    private FiltroCatalogo aFiltroCatalogo(Long idCategoria, FiltrosCatalogoPeticionDTO peticion, TipoPrecio tipoPrecio) {
        return new FiltroCatalogo(idCategoria,
                filtroValidador.terminos(peticion.texto()), false,
                filtroValidador.ids(peticion.marcas(), "marca"),
                filtroValidador.ids(peticion.subcategorias(), "subcategoría"),
                filtroValidador.ids(peticion.tallas(), "talla"),
                filtroValidador.ids(peticion.colores(), "color"),
                filtroValidador.unidades(peticion.unidadesMedida()),
                peticion.precioMin(), peticion.precioMax(),
                tipoPrecio, filtroValidador.validarOrden(peticion.orden()));
    }

    /** A2 · Primero coincidencias parciales de la línea; si no hay, sus más vendidos. */
    private List<ProductoTarjetaDTO> sugerencias(FiltroCatalogo filtro, TipoPrecio tipoPrecio) {
        var parciales = filtro.terminos().size() > 1
                ? productoDAO.buscarEnTienda(filtro.coincidenciaParcial(), 0, MAX_SUGERENCIAS)
                : new PaginaProductos(List.of(), 0);
        if (parciales.ids().isEmpty())
            parciales = productoDAO.buscarEnTienda(filtro.soloLinea().conOrden(OrdenCatalogo.MAS_VENDIDOS), 0, MAX_SUGERENCIAS);
        return tarjetas(parciales, tipoPrecio);
    }

    private List<ProductoTarjetaDTO> tarjetas(PaginaProductos pagina, TipoPrecio tipoPrecio) {
        // Con variantes: la tarjeta muestra los colores con existencia y la foto de cada uno.
        return productoDAO.obtenerParaTienda(pagina.ids(), true).stream()
                .map(p -> catalogoBOMapper.aProductoTarjetaDTO(p, precioReferencia(nivelesPrecio(p), tipoPrecio).orElse(null),
                        evaluadorStock::existencia))
                .toList();
    }
}
