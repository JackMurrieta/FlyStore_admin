package com.flystore.arquitectura;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Comprueba sin conectar a una BD que la instalación inicial incluye los índices y restricciones de 003. */
class MigracionesTest {
    private static final String INICIO = "-- 1. Índices";
    private static final String FIN = "$restricciones$;";

    @Test void esquemaInicialIncluyeIndicesYRestricciones() throws Exception {
        var migracion = leer("migraciones/003_indices_y_restricciones.sql");
        var inicial = leer("migraciones/000_esquema_inicial.sql");
        int desde = migracion.indexOf(INICIO);
        int hasta = migracion.indexOf(FIN);
        assertTrue(desde >= 0 && hasta > desde, "003 debe conservar las marcas de inicio y fin del bloque");
        var bloque = migracion.substring(desde, hasta + FIN.length());
        assertTrue(inicial.contains(bloque), "000 debe incluir el mismo bloque de índices y restricciones que 003");
        assertTrue(inicial.indexOf(bloque) > inicial.indexOf("-- FIN DDL generado."),
                "El bloque debe quedar fuera del DDL generado por Hibernate");
    }

    private static String leer(String ruta) throws Exception {
        return Files.readString(Path.of(ruta)).replace("\r\n", "\n");
    }
}
