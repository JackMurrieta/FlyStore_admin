package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.entidades.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Alta, cambio de cantidad, precio y descuento de las líneas del pedido en curso. */
@Service
public class DetallePedidoBO implements IDetallePedidoBO {

    private final IProductoVentaBO productoVentaBO;

    public DetallePedidoBO(IProductoVentaBO productoVentaBO) {
        this.productoVentaBO = productoVentaBO;
    }

    @Override
    public int agregarVariante(PedidoVenta pedido, VarianteProducto variante, int cantidad) {
        var existente = buscarPorVariante(pedido, variante.getId());
        if (existente.isPresent()) {
            var detalle = existente.get();
            detalle.setCantidad(detalle.getCantidad() + cantidad);
            return detalle.getCantidad();
        }
        var detalle = new DetallePedido();
        detalle.setPedidoVenta(pedido);
        detalle.setCantidad(cantidad);
        detalle.setDescuentoUnitario(BigDecimal.ZERO);
        detalle.setCodigosAplicados(new ArrayList<>());
        var lineaVariante = new DetallePedidoVarianteProducto();
        lineaVariante.setDetallePedido(detalle);
        lineaVariante.setVarianteProducto(variante);
        lineaVariante.setCantidadEnCombo(1);
        detalle.setVariantes(new ArrayList<>(List.of(lineaVariante)));
        lineas(pedido).add(detalle);
        return cantidad;
    }

    @Override
    public void actualizarCantidad(PedidoVenta pedido, Long varianteProductoId, int cantidad) {
        var detalle = buscarPorVariante(pedido, varianteProductoId)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Ese producto ya no está en tu carrito"));
        if (cantidad <= 0) lineas(pedido).remove(detalle); // orphanRemoval borra la línea y sus relaciones
        else detalle.setCantidad(cantidad);
    }

    @Override
    public Optional<DetallePedido> buscarPorVariante(PedidoVenta pedido, Long varianteProductoId) {
        return lineas(pedido).stream()
                .filter(d -> d.getVariantes() != null && d.getVariantes().stream()
                        .anyMatch(dv -> dv.getVarianteProducto().getId().equals(varianteProductoId)))
                .findFirst();
    }

    @Override
    public void recalcularPrecios(PedidoVenta pedido) {
        var lineasPorProducto = lineas(pedido).stream()
                .collect(Collectors.groupingBy(d -> producto(d).getId(), LinkedHashMap::new, Collectors.toList()));
        for (var lineasProducto : lineasPorProducto.values()) {
            var producto = producto(lineasProducto.get(0));
            int piezas = lineasProducto.stream().mapToInt(DetallePedido::getCantidad).sum();
            var nivel = productoVentaBO.determinarTipoPrecio(producto, piezas);
            for (var detalle : lineasProducto) {
                detalle.setPrecioUnitario(nivel.getPrecio());
                detalle.setTipoPrecio(nivel.getTipoPrecio());
                detalle.setDescuentoUnitario(codigo(detalle)
                        .map(c -> c.getMontoFijo().min(nivel.getPrecio()))
                        .orElse(BigDecimal.ZERO));
            }
        }
    }

    @Override
    public int aplicarCodigoDescuento(PedidoVenta pedido, CodigoDescuento codigo, Set<Long> productosAplicables) {
        int aplicadas = 0;
        for (var detalle : lineas(pedido)) {
            if (!productosAplicables.contains(producto(detalle).getId())) continue;
            aplicadas++;
            if (codigo(detalle).map(c -> c.getId().equals(codigo.getId())).orElse(false)) continue;
            var aplicados = codigosDe(detalle);
            aplicados.clear(); // un código por producto: el nuevo sustituye al anterior
            var relacion = new DetallePedidoCodigoDescuento();
            relacion.setDetallePedido(detalle);
            relacion.setCodigoDescuento(codigo);
            aplicados.add(relacion);
        }
        return aplicadas;
    }

    @Override
    public void quitarCodigoDescuento(PedidoVenta pedido, CodigoDescuento codigo) {
        for (var detalle : lineas(pedido)) {
            codigosDe(detalle).removeIf(r -> r.getCodigoDescuento().getId().equals(codigo.getId()));
        }
    }

    @Override
    public Collection<CodigoDescuento> obtenerCodigosAplicados(PedidoVenta pedido) {
        var porId = new LinkedHashMap<Long, CodigoDescuento>();
        for (var detalle : lineas(pedido)) codigo(detalle).ifPresent(c -> porId.putIfAbsent(c.getId(), c));
        return porId.values();
    }

    @Override
    public BigDecimal calcularSubtotal(DetallePedido detalle) {
        var precio = detalle.getPrecioUnitario() == null ? BigDecimal.ZERO : detalle.getPrecioUnitario();
        var descuento = detalle.getDescuentoUnitario() == null ? BigDecimal.ZERO : detalle.getDescuentoUnitario();
        return precio.subtract(descuento).multiply(BigDecimal.valueOf(detalle.getCantidad()));
    }

    static Producto producto(DetallePedido detalle) {
        return variante(detalle).getProducto();
    }

    static VarianteProducto variante(DetallePedido detalle) {
        if (detalle.getVariantes() == null || detalle.getVariantes().isEmpty())
            throw new IllegalStateException("Línea de pedido sin variante: " + detalle.getId());
        return detalle.getVariantes().get(0).getVarianteProducto();
    }

    static Optional<CodigoDescuento> codigo(DetallePedido detalle) {
        return detalle.getCodigosAplicados() == null ? Optional.empty()
                : detalle.getCodigosAplicados().stream().map(DetallePedidoCodigoDescuento::getCodigoDescuento).findFirst();
    }

    private static List<DetallePedidoCodigoDescuento> codigosDe(DetallePedido detalle) {
        if (detalle.getCodigosAplicados() == null) detalle.setCodigosAplicados(new ArrayList<>());
        return detalle.getCodigosAplicados();
    }

    private static List<DetallePedido> lineas(PedidoVenta pedido) {
        if (pedido.getDetalles() == null) pedido.setDetalles(new ArrayList<>());
        return pedido.getDetalles();
    }
}
