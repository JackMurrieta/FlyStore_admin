package com.flystore;

import com.flystore.config.SupabaseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Clase principal de la aplicación Spring Boot - FlyStore Backend
 *
 * Al estar en el paquete raíz com.flystore, Spring Boot automáticamente escanea:
 * - Componentes (@Component, @Service, @Controller, @RestController)
 * - Entidades JPA (@Entity)
 * - Repositorios JPA (interfaces que extienden JpaRepository)
 *
 * @author Usuario
 */
@SpringBootApplication
@EnableConfigurationProperties(SupabaseProperties.class)
public class API {

    public static void main(String[] args) {
        SpringApplication.run(API.class, args);
    }
}
