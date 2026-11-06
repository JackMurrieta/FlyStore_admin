package com.flystore.conexion;

import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConexionSupabase {

    @Bean
    CommandLineRunner probarConexion(DataSource ds) {
        return args -> {
            try (var c = ds.getConnection()) {
                System.out.println("Conectado a Supabase: " + c.getMetaData().getURL());
            }
        };
    }
}