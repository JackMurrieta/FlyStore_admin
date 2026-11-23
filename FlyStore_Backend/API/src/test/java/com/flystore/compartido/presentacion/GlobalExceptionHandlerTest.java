package com.flystore.compartido.presentacion;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rutas que no existen y métodos no permitidos responden 404 y 405 con ErrorDTO, no 500.
 * Usa un controlador mínimo para probar solo el manejador global.
 */
class GlobalExceptionHandlerTest {

    @RestController
    @RequestMapping("/api/prueba")
    static class ControladorDePrueba {

        @GetMapping("/{id}")
        String obtener(@PathVariable String id) {
            return id;
        }

        @PostMapping("/{id}/accion")
        String accion(@PathVariable String id) {
            return id;
        }

        /** Lo que hace Spring Boot en producción con una ruta desconocida. */
        @GetMapping("/estatico/archivo")
        String recursoInexistente() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "api/prueba/estatico/archivo.png");
        }

        @GetMapping("/falla/inesperada")
        String falla() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test void unaRutaQueNoExisteResponde404() throws Exception {
        mvc.perform(post("/api/prueba/1/accion-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("La ruta solicitada no existe"));
    }

    @Test void unRecursoEstaticoInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/prueba/estatico/archivo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NO_ENCONTRADO"));
    }

    @Test void unMetodoNoPermitidoResponde405ConLosMetodosQueSiAcepta() throws Exception {
        mvc.perform(delete("/api/prueba/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(jsonPath("$.error").value("METODO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.message").value("El método DELETE no está permitido en esta ruta"));

        mvc.perform(get("/api/prueba/1/accion"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")));
    }

    @Test void loDemasSigueRespondiendo500SinDetallesInternos() throws Exception {
        mvc.perform(get("/api/prueba/falla/inesperada"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERROR_INTERNO"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("detalle interno"))));
    }
}
