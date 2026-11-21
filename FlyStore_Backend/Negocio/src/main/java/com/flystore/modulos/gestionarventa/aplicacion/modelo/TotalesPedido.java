package com.flystore.modulos.gestionarventa.aplicacion.modelo;

import com.flystore.persistencia.entidades.PrecioProducto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Totales que calcula PedidoVentaBO.calcularTotales y que el mapper copia al PedidoVentaDTO.
 *
 * @param preciosBase precio BASE por pieza de cada producto del pedido, por id de producto.
 */
public record TotalesPedido(
        int totalPiezas,
        String tipoPrecioAplicado,
        String siguienteTipoPrecio,
        Integer piezasParaSiguienteNivel,
        List<PrecioProducto> nivelesPrecio,
        BigDecimal subtotalPrecioBase,
        BigDecimal ahorroPorTipoPrecio,
        BigDecimal totalDescuentos,
        BigDecimal costoEnvio,
        BigDecimal total,
        Map<Long, BigDecimal> preciosBase) {}
