package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.EstadoProducto;
import com.flystore.persistencia.entidades.TipoPrecio;
import com.flystore.modulos.catalogo.aplicacion.mappers.ProductoMapper;
import com.flystore.dominio.excepcion.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoBO implements IProductoBO {

    private final IProductoDAO productoDAO;
    private final IDetallePedidoDAO detallePedidoDAO;
    private final ProductoMapper productoMapper;

    public ProductoBO(IProductoDAO productoDAO, IDetallePedidoDAO detallePedidoDAO, ProductoMapper productoMapper) {
        this.productoDAO = productoDAO;
        this.detallePedidoDAO = detallePedidoDAO;
        this.productoMapper = productoMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> obtenerProductos() {
        return productoDAO.obtenerTodos().stream().map(productoMapper::aProductoCatalogoDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoCatalogoDTO> buscarProductos(String texto, String idCategoria, String idSubcategoria, String idMarca, String estado) {
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
    @Transactional
    public void crearProducto(CrearProductoDTO dto) {
        validarDatosProducto(dto);
        validarProductoNoDuplicado(dto.clave());
        validarStock(dto.variantes());
        validarCoherenciaPrecios(dto.preciosProducto());
        try {
            productoDAO.guardar(productoMapper.aProducto(dto));
        } catch (IllegalArgumentException e) {
            throw invalido(e.getMessage());
        }
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
        if (dto.imagenes() == null || dto.imagenes().isEmpty()) {
            throw invalido("El producto debe tener imágenes");
        }
        for (var imagen : dto.imagenes()) {
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
