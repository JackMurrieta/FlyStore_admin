package com.flystore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor de contexto de base de datos para Row Level Security (RLS)
 * Establece variables de sesión PostgreSQL que pueden ser usadas por políticas RLS
 */
@Component
public class DatabaseContextManager {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Establece el contexto de usuario en la sesión de PostgreSQL
     * Esto permite que las políticas RLS accedan a la información del usuario autenticado
     *
     * @param userId ID del usuario (UUID de Supabase Auth)
     * @param role Rol del usuario (ej: "authenticated", "anon")
     * @param claims Claims adicionales del JWT
     */
    @Transactional
    public void setUserContext(UUID userId, String role, Claims claims) {
        try {
            // Crear un mapa de claims para PostgreSQL
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("sub", userId.toString());
            claimsMap.put("role", role != null ? role : "authenticated");

            // Agregar email si está disponible
            if (claims.get("email") != null) {
                claimsMap.put("email", claims.get("email"));
            }

            // Convertir a JSON string
            String claimsJson = objectMapper.writeValueAsString(claimsMap);

            // Establecer variables de sesión PostgreSQL
            // Usando 'false' como tercer parámetro significa nivel de sesión (no transacción)
            entityManager.createNativeQuery(
                "SELECT set_config('request.jwt.claims', :claims, false)"
            )
            .setParameter("claims", claimsJson)
            .getSingleResult();

            // También establecer el role
            // Nota: Esto asume que tienes un role "authenticated" en PostgreSQL
            // Si no existe, puedes comentar esta línea
            try {
                entityManager.createNativeQuery(
                    "SET LOCAL ROLE authenticated"
                ).executeUpdate();
            } catch (Exception e) {
                // Si el role no existe, simplemente continuar
                // El RLS aún puede funcionar con las variables de sesión
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to set database context for user: " + userId, e);
        }
    }

    /**
     * Limpia el contexto de usuario de la sesión
     * Debe ser llamado al finalizar la petición o en caso de error
     */
    @Transactional
    public void clearUserContext() {
        try {
            // Limpiar la variable de sesión
            entityManager.createNativeQuery(
                "SELECT set_config('request.jwt.claims', NULL, false)"
            ).getSingleResult();

            // Resetear el role
            try {
                entityManager.createNativeQuery(
                    "RESET ROLE"
                ).executeUpdate();
            } catch (Exception e) {
                // Ignorar si falla
            }
        } catch (Exception e) {
            // Log pero no lanzar excepción en cleanup
            System.err.println("Failed to clear database context: " + e.getMessage());
        }
    }
}
