package com.flystore.persistencia;

import com.flystore.persistencia.conexion.ConexionBD;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConexionBDTest {
    @Test
    void construyePoolDesdePropiedadesSinAbrirConexion() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "spring.datasource.url", "jdbc:postgresql://localhost:5432/flystore",
                    "spring.datasource.username", "test",
                    "spring.datasource.password", "test-password",
                    "spring.datasource.hikari.maximum-pool-size", "5",
                    "spring.datasource.hikari.connection-timeout", "10000")));
            context.register(ConexionBD.class);
            context.refresh();
            var pool = context.getBean(HikariDataSource.class);
            assertEquals("jdbc:postgresql://localhost:5432/flystore", pool.getJdbcUrl());
            assertEquals("test", pool.getUsername());
            assertEquals(5, pool.getMaximumPoolSize());
            assertEquals(10000, pool.getConnectionTimeout());
            assertNull(pool.getHikariPoolMXBean());
        }
    }
}
