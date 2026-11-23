package com.flystore.contratos.administrarpedidos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * CU-04 · Comprobante de transferencia del pedido.
 *
 * @param url    enlace temporal para verlo; vacío si no se pudo generar.
 * @param monto  monto leído del comprobante; hoy siempre null, porque el archivo no se analiza y
 *               el personal lo compara a la vista contra el total del pedido.
 * @param fecha  momento en que llegó el comprobante.
 * @param estado EN_REVISION, COMPLETADO o RECHAZADO.
 */
public record ComprobanteDTO(
        String url,
        @JsonProperty("nombre_archivo") String nombreArchivo,
        BigDecimal monto,
        String fecha,
        String concepto,
        String estado) {}
