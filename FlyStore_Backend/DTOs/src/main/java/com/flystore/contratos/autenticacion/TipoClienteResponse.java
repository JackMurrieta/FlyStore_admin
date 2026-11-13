package com.flystore.contratos.autenticacion;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para devolver los tipos de cliente disponibles
 * Refleja el enum tipo_cliente de PostgreSQL/Supabase
 */
public record TipoClienteResponse(
        @JsonProperty("valor")
        String valor
) {
    /**
     * Crea un TipoClienteResponse desde el nombre del enum
     */
    public static TipoClienteResponse from(String valorEnum) {
        return new TipoClienteResponse(valorEnum);
    }
}
