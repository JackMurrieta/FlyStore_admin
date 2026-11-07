package com.flystore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Inicializador de Row Level Security (RLS)
 * Se ejecuta automáticamente después de que Hibernate crea las tablas
 * Lee y ejecuta el script data.sql para habilitar RLS y crear políticas
 */
@Component
public class RLSInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RLSInitializer.class);

    @Autowired
    private DataSource dataSource;

    private boolean initialized = false;

    /**
     * Se ejecuta cuando el contexto de Spring ha terminado de cargarse
     * En este punto, Hibernate ya ha creado las tablas
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Evitar ejecución múltiple en caso de refresh del contexto
        if (initialized) {
            logger.info("RLS ya fue inicializado, omitiendo...");
            return;
        }

        try {
            logger.info("=".repeat(60));
            logger.info("Iniciando configuración de Row Level Security (RLS)...");
            logger.info("=".repeat(60));

            // Obtener conexión a la base de datos
            try (Connection connection = dataSource.getConnection()) {
                // Cargar y ejecutar el script data.sql
                ClassPathResource resource = new ClassPathResource("data.sql");

                if (!resource.exists()) {
                    logger.warn("Archivo data.sql no encontrado. RLS no será configurado.");
                    return;
                }

                logger.info("Ejecutando script: data.sql");

                // Ejecutar el script SQL
                ScriptUtils.executeSqlScript(connection, resource);

                logger.info("✓ Row Level Security habilitado exitosamente");
                logger.info("✓ Funciones helper creadas (auth.uid, is_admin, is_staff)");
                logger.info("✓ Políticas RLS de desarrollo aplicadas");
                logger.info("=".repeat(60));

                initialized = true;
            }

        } catch (Exception e) {
            logger.error("=".repeat(60));
            logger.error("ERROR al configurar Row Level Security", e);
            logger.error("La aplicación continuará pero RLS puede no estar configurado");
            logger.error("=".repeat(60));
            // No lanzamos la excepción para que la aplicación pueda continuar
        }
    }
}
