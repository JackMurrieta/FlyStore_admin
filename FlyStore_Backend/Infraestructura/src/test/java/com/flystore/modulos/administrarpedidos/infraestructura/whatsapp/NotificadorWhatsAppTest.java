package com.flystore.modulos.administrarpedidos.infraestructura.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NotificadorWhatsAppTest {
    private static final String URL = "https://graph.example/v1/123/messages";

    @Test void sinProveedorConfiguradoNoEnviaYDevuelveFalse() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        assertFalse(new NotificadorWhatsApp("", "", builder.build()).notificar("6441234567", "Hola"));
        assertFalse(new NotificadorWhatsApp(URL, " ", builder.build()).notificar("6441234567", "Hola"));
        assertFalse(new NotificadorWhatsApp(null, null, builder.build()).notificar("6441234567", "Hola"));
        server.verify();
    }

    @Test void conProveedorEnviaElTextoAlNumeroConLada() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-prueba"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("526441234567"))
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.text.body").value("Hola Ana"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertTrue(new NotificadorWhatsApp(URL, "token-prueba", builder.build()).notificar("644 123 4567", "Hola Ana"));
        server.verify();
    }

    @Test void siElProveedorFallaOFaltanDatosDevuelveFalseSinLanzar() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withServerError());
        var notificador = new NotificadorWhatsApp(URL, "token-prueba", builder.build());
        assertFalse(notificador.notificar("6441234567", "Hola"));
        assertFalse(notificador.notificar(null, "Hola"), "Sin teléfono no hay a quién avisar");
        assertFalse(notificador.notificar("6441234567", " "));
        server.verify();
    }
}
