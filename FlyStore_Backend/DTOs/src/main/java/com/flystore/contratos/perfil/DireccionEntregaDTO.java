package com.flystore.contratos.perfil;

/**
 * Dirección de entrega guardada por el cliente (tabla datos_entrega, solo las activas).
 *
 * @param predeterminada true si es la que se propone al hacer un pedido.
 */
public record DireccionEntregaDTO(
        Long id,
        String nombreCompleto,
        String telefono,
        String ciudad,
        String estado,
        String direccion,
        String referencias,
        boolean predeterminada) {}
