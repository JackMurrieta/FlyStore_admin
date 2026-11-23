package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.Size;

/** CU-02 · Body de POST /api/ventas/pedidos/{folio}/cancelar. El motivo es opcional. */
public record CancelarPedidoPeticionDTO(
        @Size(max = 300, message = "El motivo es demasiado largo") String motivo) {}
