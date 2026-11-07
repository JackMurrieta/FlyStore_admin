package com.flystore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuración de RestClient para llamadas HTTP a Supabase
 * GRASP: Pure Fabrication - Centraliza la creación del cliente HTTP
 */
@Configuration
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
