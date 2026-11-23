package com.flystore.modulos.gestionarventa.aplicacion.modelo;

import com.flystore.contratos.administrarpedidos.DetallePedidoAdminDTO;

/**
 * Lo que deja una acción de CU-04 al terminar su transacción: cómo quedó el pedido y, si hay que
 * avisar al cliente, a qué teléfono y con qué mensaje. El aviso se envía después, ya fuera de la
 * transacción.
 *
 * @param mensaje null cuando la acción no avisa al cliente (pago en efectivo en tienda).
 */
public record ResultadoAccion(DetallePedidoAdminDTO pedido, String telefono, String mensaje) {}
