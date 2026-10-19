package Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para verificar el estado de la aplicación y la conexión a la BD
 * @author Usuario
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Endpoint simple para verificar que la API está funcionando
     * URL: http://localhost:8080/api/health
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "API funcionando correctamente");
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para verificar la conexión a la base de datos
     * URL: http://localhost:8080/api/health/db
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> checkDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);

            response.put("database", "Supabase PostgreSQL");
            response.put("connected", isValid);
            response.put("catalog", connection.getCatalog());
            response.put("url", connection.getMetaData().getURL());
            response.put("user", connection.getMetaData().getUserName());
            response.put("status", isValid ? "CONNECTED" : "ERROR");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("connected", false);
            response.put("error", e.getMessage());
            response.put("message", "No se pudo conectar a la base de datos. Verifica tus credenciales en application.properties");

            return ResponseEntity.status(500).body(response);
        }
    }
}
