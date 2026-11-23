package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.dao.interfaces.IVarianteProductoDAO;
import com.flystore.persistencia.entidades.EstadoProducto;
import com.flystore.persistencia.entidades.PedidoVenta;
import com.flystore.persistencia.entidades.VarianteProducto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Disponibilidad y descuento de existencia de las variantes vendidas. */
@Service
public class VarianteProductoBO implements IVarianteProductoBO {

    private static final Set<EstadoProducto> EN_VENTA = Set.of(EstadoProducto.PUBLICADO, EstadoProducto.STOCK_BAJO);

    private final IVarianteProductoDAO varianteProductoDAO;

    public VarianteProductoBO(IVarianteProductoDAO varianteProductoDAO) {
        this.varianteProductoDAO = varianteProductoDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public VarianteProducto obtenerVarianteEnVenta(Long varianteProductoId) {
        var variante = varianteProductoDAO.obtenerPorId(varianteProductoId)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "El producto no existe"));
        if (!EN_VENTA.contains(variante.getProducto().getEstadoProducto()))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El producto " + variante.getProducto().getNombre() + " ya no está a la venta");
        return variante;
    }

    @Override
    public void validarExistencia(VarianteProducto variante, int cantidad) {
        int stock = variante.getStock() == null ? 0 : variante.getStock();
        if (cantidad > stock) {
            var nombre = describir(variante);
            throw new NegocioException(TipoError.DATOS_INVALIDOS, stock <= 0
                    ? "Ya no hay piezas disponibles de " + nombre
                    : "Solo hay " + stock + (stock == 1 ? " pieza disponible de " : " piezas disponibles de ") + nombre);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarExistencia(PedidoVenta pedido) {
        piezasPorVariante(pedido).forEach((id, piezas) -> validarExistencia(obtenerVarianteEnVenta(id), piezas));
    }

    @Override
    @Transactional
    public void descontarExistencia(PedidoVenta pedido) {
        // TreeMap: las variantes se bloquean siempre en el mismo orden (por id) para evitar bloqueos cruzados.
        piezasPorVariante(pedido).forEach((id, piezas) -> {
            var variante = varianteProductoDAO.bloquearPorId(id)
                    .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Un producto del pedido ya no existe"));
            int stock = variante.getStock() == null ? 0 : variante.getStock();
            if (piezas > stock)
                throw new NegocioException(TipoError.CONFLICTO_DE_DATOS, "No hay piezas suficientes de " + describir(variante)
                        + ": el pedido necesita " + piezas + " y hay " + stock + ". Contacta al cliente para ajustar su pedido.");
            variante.setStock(stock - piezas);
        });
    }

    @Override
    @Transactional
    public void reponerExistencia(PedidoVenta pedido) {
        piezasPorVariante(pedido).forEach((id, piezas) -> {
            var variante = varianteProductoDAO.bloquearPorId(id)
                    .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Un producto del pedido ya no existe"));
            variante.setStock((variante.getStock() == null ? 0 : variante.getStock()) + piezas);
        });
    }

    /** Piezas por variante: cantidad de la línea × cantidad de la variante en el combo (1 en líneas simples). */
    private static Map<Long, Integer> piezasPorVariante(PedidoVenta pedido) {
        var piezas = new TreeMap<Long, Integer>();
        for (var detalle : pedido.getDetalles()) {
            for (var dv : detalle.getVariantes()) {
                int porCombo = dv.getCantidadEnCombo() == null ? 1 : dv.getCantidadEnCombo();
                piezas.merge(dv.getVarianteProducto().getId(), detalle.getCantidad() * porCombo, Integer::sum);
            }
        }
        return piezas;
    }

    private static String describir(VarianteProducto variante) {
        var texto = new StringBuilder(variante.getProducto().getNombre());
        if (variante.getColor() != null && variante.getColor().getNombreColor() != null)
            texto.append(" ").append(variante.getColor().getNombreColor().toLowerCase(java.util.Locale.ROOT));
        if (variante.getTalla() != null && variante.getTalla().getEtiqueta() != null)
            texto.append(" talla ").append(variante.getTalla().getEtiqueta());
        return texto.toString();
    }
}
