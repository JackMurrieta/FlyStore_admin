package CU_iniciarSesion_DTOs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO record para respuesta de Supabase Auth
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseSession(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        Integer expiresIn,

        @JsonProperty("expires_at")
        Long expiresAt,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("user")
        SupabaseUser user
) {
}
