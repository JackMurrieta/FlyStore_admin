package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.entidades.PrecioProducto;
import com.flystore.persistencia.entidades.Producto;
import com.flystore.persistencia.entidades.TipoPrecio;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Reglas de precio por volumen. No consulta la base: trabaja con los precios ya cargados del producto. */
@Service
public class ProductoVentaBO implements IProductoVentaBO {

    /** Mayor mínimo primero; con el mismo mínimo, el precio más bajo. */
    private static final Comparator<PrecioProducto> MEJOR_NIVEL = Comparator
            .comparing(PrecioProducto::getMinPiezas).reversed()
            .thenComparing(PrecioProducto::getPrecio);

    @Override
    public PrecioProducto determinarTipoPrecio(Producto producto, int totalPiezas) {
        return obtenerNivelesPrecio(producto).stream()
                .filter(p -> p.getMinPiezas() <= totalPiezas)
                .min(MEJOR_NIVEL)
                .orElseGet(() -> obtenerNivelBase(producto));
    }

    @Override
    public BigDecimal obtenerPrecioBase(Producto producto) {
        return obtenerNivelBase(producto).getPrecio();
    }

    @Override
    public Optional<PrecioProducto> obtenerSiguienteTipoPrecio(Producto producto, int totalPiezas) {
        return obtenerNivelesPrecio(producto).stream()
                .filter(p -> p.getMinPiezas() > totalPiezas)
                .findFirst();
    }

    @Override
    public List<PrecioProducto> obtenerNivelesPrecio(Producto producto) {
        var precios = producto.getPrecios() == null ? List.<PrecioProducto>of() : producto.getPrecios();
        var validos = precios.stream()
                .filter(p -> p.getMinPiezas() != null && p.getPrecio() != null && p.getTipoPrecio() != null)
                .sorted(Comparator.comparing(PrecioProducto::getMinPiezas).thenComparing(PrecioProducto::getPrecio))
                .toList();
        if (validos.isEmpty())
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El producto " + producto.getNombre() + " no tiene precio y no se puede vender");
        return validos;
    }

    private PrecioProducto obtenerNivelBase(Producto producto) {
        var niveles = obtenerNivelesPrecio(producto);
        return niveles.stream().filter(p -> p.getTipoPrecio() == TipoPrecio.BASE).findFirst().orElse(niveles.get(0));
    }
}
