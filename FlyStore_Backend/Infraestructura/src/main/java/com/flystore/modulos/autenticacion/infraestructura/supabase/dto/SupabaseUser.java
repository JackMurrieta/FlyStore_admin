package com.flystore.modulos.autenticacion.infraestructura.supabase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO record para usuario de Supabase Auth
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseUser(
        @JsonProperty("id")
        String id,
        @JsonProperty("email")
        String email,
        @JsonProperty("email_confirmed_at")
        String emailConfirmedAt,
        @JsonProperty("phone")
        String phone,
        @JsonProperty("created_at")
        String createdAt,
        @JsonProperty("updated_at")
        String updatedAt,
        @JsonProperty("role")
        String role,
        @JsonProperty("user_metadata")
        Map<String, Object> userMetadata,
        @JsonProperty("app_metadata")
        Map<String, Object> appMetadata
        ) {

}
