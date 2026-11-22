package com.flystore.contratos.gestionarventa;

import java.math.BigDecimal;

/**
 * Código de descuento aplicado a una línea. Un código por producto.
 *
 * @param etiquetaValor  descripción del valor del código, por ejemplo «$20 por pieza».
 * @param montoDescuento descuentoUnitario × cantidad.
 */
public record CodigoDescuentoAplicadoDTO(String codigo, String etiquetaValor, BigDecimal montoDescuento) {}
