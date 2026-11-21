package com.flystore.contratos.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos personales que el usuario puede editar. El correo se cambia aparte (requiere confirmación). */
public record ActualizarPerfilPeticionDTO(
        @NotBlank(message = "Escribe tu nombre") @Size(max = 80, message = "El nombre es muy largo") String nombre,
        @NotBlank(message = "Escribe tus apellidos") @Size(max = 120, message = "Los apellidos son muy largos") String apellidos,
        @Pattern(regexp = "^$|\\d{10}", message = "El teléfono debe tener 10 dígitos") String telefono) {}
