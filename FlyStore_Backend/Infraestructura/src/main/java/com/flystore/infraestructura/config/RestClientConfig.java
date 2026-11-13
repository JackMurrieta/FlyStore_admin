package com.flystore.infraestructura.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.client.RestClient;

/**
 * Configuración de RestClient para llamadas HTTP a Supabase GRASP: Pure
 * Fabrication - Centraliza la creación del cliente HTTP
 */
@Configuration
@EnableConfigurationProperties(SupabaseProperties.class)
public class RestClientConfig {

    private final SupabaseProperties supabaseProperties;

    public RestClientConfig(SupabaseProperties supabaseProperties) {
        this.supabaseProperties = supabaseProperties;
    }

    @Bean
    public RestClient supabaseRestClient() {
        return RestClient.builder()
                .baseUrl(supabaseProperties.getUrl())
                .defaultHeader("apikey", supabaseProperties.getPublishableKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
