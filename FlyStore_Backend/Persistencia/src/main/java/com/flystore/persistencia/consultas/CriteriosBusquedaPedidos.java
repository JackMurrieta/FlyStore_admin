package com.flystore.persistencia.consultas;

import com.flystore.persistencia.entidades.EstadoVenta;
import java.time.OffsetDateTime;

/**
 * Filtros de la lista de pedidos del personal (CU-04). Todo campo null se ignora.
 *
 * @param texto      texto escrito por el personal: parte de un folio, o el nombre o teléfono de un cliente.
 * @param estado     estado exacto del pedido.
 * @param metodoPago TRANSFERENCIA o EFECTIVO.
 * @param desde      registrados a partir de este instante (incluido).
 * @param hasta      registrados antes de este instante (excluido).
 * @param pagina     página pedida, desde 1.
 */
public record CriteriosBusquedaPedidos(String texto, EstadoVenta estado, String metodoPago,
                                       OffsetDateTime desde, OffsetDateTime hasta, int pagina) {

    public static final String TRANSFERENCIA = "TRANSFERENCIA";
    public static final String EFECTIVO = "EFECTIVO";
    /** Pedidos por página. */
    public static final int TAMANO_PAGINA = 50;

    public CriteriosBusquedaPedidos {
        pagina = Math.max(pagina, 1);
    }
}
