package com.flystore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
public class API {

    public static void main(String[] args) {
        SpringApplication.run(API.class, args);
    }
}
