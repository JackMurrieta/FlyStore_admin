package org.itson.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Clase principal de la aplicación Spring Boot
 * @author Usuario
 */
@SpringBootApplication
@ComponentScan(basePackages = {"org.itson.api", "Controller", "Entity"})
@EntityScan(basePackages = {"Entity"})
public class API {

    public static void main(String[] args) {
        SpringApplication.run(API.class, args);
    }
}
