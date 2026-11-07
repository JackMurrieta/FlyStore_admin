package com.flystore.config;

import com.flystore.security.SupabaseJwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad de Spring Security
 * Integra el filtro de JWT de Supabase y define las reglas de autorización
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SupabaseJwtFilter supabaseJwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF ya que usamos JWT (stateless)
            .csrf(csrf -> csrf.disable())

            // Configurar sesión como stateless (sin sesiones HTTP)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Configurar reglas de autorización
            .authorizeHttpRequests(auth -> auth
                // MODO DESARROLLO: Permitir todas las rutas sin autenticación
                // TODO: En producción, descomentar las reglas restrictivas abajo
                .anyRequest().permitAll()

                // MODO PRODUCCIÓN: Descomentar estas líneas y comentar la línea de arriba
                // .requestMatchers("/api/public/**").permitAll()
                // .requestMatchers("/health").permitAll()
                // .requestMatchers("/actuator/**").permitAll()
                // .requestMatchers("/api/admin/**").hasRole("AUTHENTICATED")
                // .anyRequest().authenticated()
            )

            // Agregar filtro de JWT antes del filtro de autenticación estándar
            .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
