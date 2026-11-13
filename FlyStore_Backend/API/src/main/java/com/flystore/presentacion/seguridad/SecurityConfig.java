package com.flystore.presentacion.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad de Spring Security
 * SOLID: SRP - Solo configuración de seguridad
 * Backend actúa como wrapper de Supabase:
 * - Valida JWT de Supabase usando OAuth2 Resource Server
 * - Frontend NO tiene acceso directo a Supabase
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final SupabaseJwtConverter jwtConverter;
    private final CookieJwtFilter cookieJwtFilter;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public SecurityConfig(SupabaseJwtConverter jwtConverter, CookieJwtFilter cookieJwtFilter) {
        this.jwtConverter = jwtConverter;
        this.cookieJwtFilter = cookieJwtFilter;
    }

    @Bean
    public FilterRegistrationBean<CookieJwtFilter> cookieJwtFilterRegistration() {
        var registration = new FilterRegistrationBean<>(cookieJwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF (usamos JWT stateless)
            .csrf(csrf -> csrf.disable())

            // Configurar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless (sin sesiones HTTP)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Reglas de autorización
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/health", "/actuator/health").permitAll()

                .requestMatchers("/api/admin/**", "/api/productos", "/api/productos/**", "/api/catalogo/**").hasAnyRole("ADMIN", "EMPLEADO")
                .anyRequest().authenticated()
            )

            // 🆕 Agregar filtro que extrae JWT desde cookies HttpOnly
            // DEBE ir ANTES de OAuth2ResourceServer para que el token esté disponible
            .addFilterBefore(cookieJwtFilter, UsernamePasswordAuthenticationFilter.class)

            // Configurar OAuth2 Resource Server para validar JWT de Supabase
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
            );

        return http.build();
    }

    /**
     * Configuración CORS centralizada
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
