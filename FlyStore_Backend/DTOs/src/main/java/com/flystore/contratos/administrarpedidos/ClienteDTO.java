package com.flystore.contratos.administrarpedidos;

/** CU-04 · Cliente del pedido. El teléfono es el de la cuenta o, si no tiene, el de la entrega. */
public record ClienteDTO(Long id, String nombre, String telefono) {}
