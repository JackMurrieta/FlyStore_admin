package com.flystore.arquitectura;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapeoJpaTest {
    @Test
    void asociacionesYHerenciaSonCoherentesSinConectarALaBase() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.temp.use_jdbc_metadata_defaults", false)
                .applySetting("hibernate.connection.provider_class", "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
                .build();
        try {
            var sources = new MetadataSources(registry);
            var count = 0;
            try (var paths = Files.walk(Path.of("src/main/java/com/flystore/persistencia/entidades"))) {
                for (var file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var name = file.getFileName().toString().replace(".java", "");
                    var type = Class.forName("com.flystore.persistencia.entidades." + name);
                    if (type.isAnnotationPresent(Entity.class)) {
                        sources.addAnnotatedClass(type);
                        count++;
                    }
                }
            }
            assertTrue(count > 0);
            var metadata = (MetadataImplementor) sources.buildMetadata();
            metadata.validate();
            try (var factory = (org.hibernate.engine.spi.SessionFactoryImplementor) metadata.buildSessionFactory()) {
                var translator = factory.getQueryEngine().getHqlTranslator();
                try (var daos = Files.walk(Path.of("src/main/java/com/flystore/persistencia/dao"))) {
                    var pattern = java.util.regex.Pattern.compile("\"(select [^\"]+)\"");
                    for (var file : daos.filter(p -> p.toString().endsWith(".java")).toList()) {
                        var matcher = pattern.matcher(Files.readString(file));
                        while (matcher.find()) translator.translate(matcher.group(1), null);
                    }
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
