package com.flystore.contratos.gestionarventa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrega del cliente. En la respuesta pueden venir vacíos (cliente nuevo);
 * al registrar el pedido se validan.
 */
public record DatosEntregaDTO(
        @NotBlank(message = "Escribe el nombre de quien recibe") @Size(max = 255) String nombreCompleto,
        @NotBlank(message = "Escribe un teléfono") @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos") String telefono,
        @NotBlank(message = "Escribe la ciudad") @Size(max = 255) String ciudad,
        @NotBlank(message = "Escribe el estado") @Size(max = 255) String estado,
        @NotBlank(message = "Escribe la dirección") @Size(max = 255) String direccion,
        @Size(max = 255) String referencias,
        boolean guardarComoPredeterminada) {}
