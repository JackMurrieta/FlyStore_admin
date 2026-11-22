package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.modulos.catalogo.aplicacion.mappers.*;
import com.flystore.modulos.catalogo.aplicacion.modelo.CriteriosFiltro;
import com.flystore.modulos.catalogo.aplicacion.modelo.ProductoDetalleCompleto;
import com.flystore.persistencia.entidades.Categoria;
import com.flystore.persistencia.entidades.EstadoProducto;
import com.flystore.persistencia.entidades.Marca;
import com.flystore.persistencia.entidades.Producto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoBO implements ICatalogoBO {

    private final ICategoriaDAO categoriaDAO;
    private final ISubcategoriaDAO subcategoriaDAO;
    private final IMarcaDAO marcaDAO;
    private final ITallaDAO tallaDAO;
    private final IColorDAO colorDAO;
    private final IProductoDAO productoDAO;
    private final CatalogoMapper catalogoMapper;
    private final CatalogoBOMapper catalogoBOMapper;
    private final FiltroValidador filtroValidador;
    private final EvaluadorStock evaluadorStock;

    public CatalogoBO(ICategoriaDAO categoriaDAO, ISubcategoriaDAO subcategoriaDAO, IMarcaDAO marcaDAO, ITallaDAO tallaDAO, IColorDAO colorDAO, IProductoDAO productoDAO, CatalogoMapper catalogoMapper, CatalogoBOMapper catalogoBOMapper, FiltroValidador filtroValidador, EvaluadorStock evaluadorStock) {
        this.categoriaDAO = categoriaDAO;
        this.subcategoriaDAO = subcategoriaDAO;
        this.marcaDAO = marcaDAO;
        this.tallaDAO = tallaDAO;
        this.colorDAO = colorDAO;
        this.productoDAO = productoDAO;
        this.catalogoMapper = catalogoMapper;
        this.catalogoBOMapper = catalogoBOMapper;
        this.filtroValidador = filtroValidador;
        this.evaluadorStock = evaluadorStock;
    }

    @Override
    public FiltrosDisponiblesDTO obtenerFiltrosDisponibles() {
        return catalogoMapper.aFiltrosDisponiblesDTO(categoriaDAO.obtenerTodas(), subcategoriaDAO.obtenerTodas(), marcaDAO.obtenerTodas(), colorDAO.obtenerTodos());
    }

    @Override
    public List<TallaDTO> obtenerTallasPorCategoria(String idCategoria) {
        return tallaDAO.obtenerPorCategoria(ProductoMapper.id(idCategoria)).stream().map(catalogoMapper::aTallaDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoRespuestaDTO obtenerCatalogoInicial() {
        List<Producto> productos = productoDAO.buscar(null, null, null, null, EstadoProducto.PUBLICADO);
        List<Categoria> categorias = categoriaDAO.obtenerTodas();
        List<Marca> marcas = marcaDAO.obtenerTodas();

        OpcionesFiltroRespuestaDTO opciones = catalogoBOMapper.aOpcionesFiltroRespuestaDTO(categorias, marcas);
        return catalogoBOMapper.aCatalogoRespuestaDTO(productos, opciones);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoRespuestaDTO buscarProductos(String termino, String idCategoria) {
        CriteriosFiltro criterios = catalogoBOMapper.aCriteriosFiltro(termino, idCategoria);

        if (!filtroValidador.validarCriterios(criterios)) {
            throw new NegocioException(
                    TipoError.DATOS_INVALIDOS,
                    "Debe proporcionar al menos un criterio de búsqueda válido.");
        }

        List<Producto> filtrados = productoDAO.buscar(
                criterios.termino(),
                criterios.idCategoria(),
                null,
                null,
                EstadoProducto.PUBLICADO
        );
        return catalogoBOMapper.aCatalogoRespuestaDTO(filtrados);
    }

    @Override
    @Transactional
    public ProductoDetalleRespuestaDTO obtenerDetalleProducto(Long idProducto) {
        Producto producto = productoDAO.obtenerPorId(idProducto)
                .orElseThrow(() -> new NegocioException(
                TipoError.RECURSO_NO_ENCONTRADO,
                "Producto no encontrado con id: " + idProducto));

        boolean disponible = evaluadorStock.calcularDisponibilidad(producto.getVariantes());
        ProductoDetalleCompleto detalle = new ProductoDetalleCompleto(producto, disponible);
        return catalogoBOMapper.aProductoDetalleRespuestaDTO(detalle);
    }
}
