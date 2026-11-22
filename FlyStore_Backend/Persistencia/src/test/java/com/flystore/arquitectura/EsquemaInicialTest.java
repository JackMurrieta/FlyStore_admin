package com.flystore.arquitectura;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Exporta el DDL sin conexión y comprueba que el script inicial corresponde al modelo actual. */
class EsquemaInicialTest {
    @TempDir Path temporal;

    @Test void esquemaInicialCoincideConLasEntidades() throws Exception {
        Path ddl = temporal.resolve("modelo.sql");
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.temp.use_jdbc_metadata_defaults", false)
                .applySetting("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("jakarta.persistence.schema-generation.database.action", "none")
                .applySetting("jakarta.persistence.schema-generation.scripts.action", "create")
                .applySetting("jakarta.persistence.schema-generation.scripts.create-target", ddl.toAbsolutePath().toString())
                .applySetting("hibernate.hbm2ddl.delimiter", ";")
                .applySetting("hibernate.format_sql", true)
                .build();
        try {
            var sources = new MetadataSources(registry);
            try (var paths = Files.list(Path.of("src/main/java/com/flystore/persistencia/entidades"))) {
                for (var file : paths.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                    var type = Class.forName("com.flystore.persistencia.entidades." + file.getFileName().toString().replace(".java", ""));
                    if (type.isAnnotationPresent(Entity.class)) sources.addAnnotatedClass(type);
                }
            }
            try (var factory = sources.buildMetadata().buildSessionFactory()) {
                assertTrue(factory.isOpen());
            }
            var generado = Files.readString(ddl).replace("\r\n", "\n").trim();
            Path exportado = Path.of("target/esquema-generado.sql");
            Files.writeString(exportado, generado);
            assertTrue(generado.contains("create table producto"));
            assertFalse(generado.toLowerCase().contains("drop table"));
            if (!Boolean.getBoolean("flystore.exportarEsquema")) {
                var inicial = Files.readString(Path.of("migraciones/000_esquema_inicial.sql")).replace("\r\n", "\n");
                assertTrue(inicial.contains(generado), "El DDL inicial debe actualizarse con las entidades");
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
