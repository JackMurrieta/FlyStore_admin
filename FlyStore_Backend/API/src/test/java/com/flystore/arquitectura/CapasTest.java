package com.flystore.arquitectura;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import static org.junit.jupiter.api.Assertions.*;

/** Reglas sobre fuentes: detectan dependencias prohibidas incluso sin iniciar Spring o una BD. */
class CapasTest {
    private static final List<String> MODULOS = List.of(
            "DTOs", "Negocio", "Persistencia", "Infraestructura", "API");

    @Test
    void dependenciasApuntanHaciaElInterior() throws Exception {
        var imports = Pattern.compile("(?m)^import\\s+(?:static\\s+)?([^;]+);");
        for (var modulo : MODULOS) {
        var root = Path.of("..", modulo, "src/main/java");
        try (var paths = Files.walk(root)) {
            for (var file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                var source = Files.readString(file);
                var packageName = source.substring(source.indexOf("package ") + 8, source.indexOf(';'));
                assertTrue(file.toString().replace('\\', '/').endsWith(
                        packageName.replace('.', '/') + "/" + file.getFileName()), file.toString());
                var matcher = imports.matcher(source);
                while (matcher.find()) {
                    var dependency = matcher.group(1);
                    if (packageName.contains(".dominio.")) {
                        assertFalse(List.of("com.flystore.modulos.", "com.flystore.infraestructura.",
                                "com.flystore.compartido.", "com.flystore.contratos.", "org.springframework.",
                                "jakarta.servlet.", "com.fasterxml.jackson.").stream().anyMatch(dependency::startsWith),
                                file + " depende de " + dependency);
                    }
                    if (modulo.equals("Persistencia")) {
                        assertFalse(dependency.contains(".aplicacion") || dependency.contains(".presentacion")
                                || dependency.startsWith("com.flystore.contratos.")
                                || dependency.startsWith("com.flystore.dominio.excepcion."), file + " depende de " + dependency);
                    }
                    if (packageName.contains(".aplicacion")) {
                        assertFalse(dependency.startsWith("com.flystore.persistencia.dao.")
                                && !dependency.startsWith("com.flystore.persistencia.dao.interfaces."), file.toString());
                        assertFalse(dependency.startsWith("com.flystore.persistencia.repositorios.")
                                || dependency.startsWith("com.flystore.persistencia.conexion."), file.toString());
                        assertFalse(dependency.contains(".infraestructura.") || dependency.contains(".presentacion.")
                                || dependency.startsWith("org.springframework.data.")
                                || dependency.startsWith("org.springframework.web.")
                                || dependency.startsWith("jakarta.servlet."), file + " depende de " + dependency);
                    }
                    if (packageName.contains(".presentacion") && !packageName.equals("com.flystore.presentacion.seguridad")) {
                        assertFalse(dependency.contains(".infraestructura.") || dependency.startsWith("com.flystore.persistencia."),
                                file + " depende de " + dependency);
                    }
                }
            }
        }
        }
    }

    @Test
    void contratosNoDependenDelBackend() throws Exception {
        try (var paths = Files.walk(Path.of("../DTOs/src/main/java"))) {
            for (var file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("import com.flystore.modulos."), file.toString());
            }
        }
    }

    @Test
    void cadaClasePerteneceAlProyectoDeSuCapa() throws Exception {
        for (var modulo : MODULOS) {
            try (var paths = Files.walk(Path.of("..", modulo, "src/main/java"))) {
                for (var file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var code = Files.readString(file);
                    var pkg = code.substring(code.indexOf("package ") + 8, code.indexOf(';'));
                    String esperado;
                    if (pkg.startsWith("com.flystore.contratos.")) esperado = "DTOs";
                    else if (pkg.startsWith("com.flystore.persistencia.")) esperado = "Persistencia";
                    else if (pkg.contains(".aplicacion") || pkg.equals("com.flystore.dominio.excepcion")) esperado = "Negocio";
                    else if (pkg.contains(".infraestructura.supabase") || Set.of("SupabaseProperties.java", "RestClientConfig.java")
                            .contains(file.getFileName().toString())) esperado = "Infraestructura";
                    else esperado = "API";
                    assertEquals(esperado, modulo, file.toString());
                }
            }
        }
    }

    @Test
    void dependenciasMavenRespetanElOrdenDeLasCapas() throws Exception {
        var permitidas = Map.of(
                "DTOs", Set.<String>of(),
                "Negocio", Set.of("Persistencia", "DTOs"),
                "Persistencia", Set.<String>of(),
                "Infraestructura", Set.of("Negocio"),
                "API", Set.of("Negocio", "DTOs", "Infraestructura"));
        for (var modulo : MODULOS) {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var pom = factory.newDocumentBuilder().parse(Path.of("..", modulo, "pom.xml").toFile());
            var dependencies = pom.getElementsByTagName("dependency");
            var encontradas = new java.util.HashSet<String>();
            for (var i = 0; i < dependencies.getLength(); i++) {
                var dep = (Element) dependencies.item(i);
                var group = dep.getElementsByTagName("groupId").item(0).getTextContent();
                if (!Set.of("org.itson", "${project.groupId}").contains(group)) continue;
                var artifact = dep.getElementsByTagName("artifactId").item(0).getTextContent();
                encontradas.add(artifact);
                if (modulo.equals("API") && artifact.equals("Infraestructura")) {
                    assertEquals("runtime", dep.getElementsByTagName("scope").item(0).getTextContent());
                }
            }
            assertEquals(permitidas.get(modulo), encontradas, modulo);
        }
    }
}
