package com.flystore.contratos.administrarpedidos;

import jakarta.validation.constraints.Size;

/** CU-04 · Body de POST /api/admin/pedidos/{folio}/cancelar. El motivo es obligatorio. */
public record CancelarPedidoPeticionDTO(
        @Size(max = 500, message = "El motivo es demasiado largo") String motivo) {}
