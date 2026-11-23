package com.flystore.contratos.administrarpedidos;

import jakarta.validation.constraints.Size;

/**
 * CU-04 · Body de POST /api/admin/pedidos/{folio}/rechazar-comprobante.
 *
 * @param motivo  por qué se rechaza; obligatorio.
 * @param mensaje texto opcional que se agrega al aviso para el cliente.
 */
public record RechazarComprobantePeticionDTO(
        @Size(max = 200, message = "El motivo es demasiado largo") String motivo,
        @Size(max = 300, message = "El mensaje es demasiado largo") String mensaje) {}
