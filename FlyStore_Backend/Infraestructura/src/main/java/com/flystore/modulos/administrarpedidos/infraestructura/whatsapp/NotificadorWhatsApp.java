package com.flystore.modulos.administrarpedidos.infraestructura.whatsapp;

import com.flystore.modulos.administrarpedidos.aplicacion.puertos.INotificadorCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Map;

/**
 * Aviso al cliente por WhatsApp (CU-04).
 *
 * <p>Hoy la tienda no tiene contratado un proveedor: sin {@code app.whatsapp.api-url} y
 * {@code app.whatsapp.token} este notificador no envía nada y devuelve false, y el frontend ofrece
 * al personal el botón «Notificar por WhatsApp» con el mensaje ya escrito.
 *
 * <p>Con ambas propiedades configuradas envía el texto con el formato de mensajes de la API de
 * WhatsApp Cloud (la URL es la de «messages» del número de la tienda). Ese camino no se ha probado
 * contra el servicio real; esa API solo entrega texto libre dentro de las 24 h posteriores al
 * último mensaje del cliente, así que para avisos fuera de esa ventana habrá que usar plantillas.
 */
@Component
public class NotificadorWhatsApp implements INotificadorCliente {

    private static final Logger logger = LoggerFactory.getLogger(NotificadorWhatsApp.class);
    /** Lada de México: se antepone a los teléfonos de 10 dígitos. */
    private static final String LADA_MX = "52";

    private final String apiUrl;
    private final String token;
    private final RestClient cliente;

    @Autowired
    public NotificadorWhatsApp(@Value("${app.whatsapp.api-url:}") String apiUrl, @Value("${app.whatsapp.token:}") String token) {
        this(apiUrl, token, crearCliente());
    }

    NotificadorWhatsApp(String apiUrl, String token, RestClient cliente) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.token = token == null ? "" : token.trim();
        this.cliente = cliente;
    }

    private static RestClient crearCliente() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean notificar(String telefono, String mensaje) {
        if (apiUrl.isEmpty() || token.isEmpty()) return false;
        var numero = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (numero.isEmpty() || mensaje == null || mensaje.isBlank()) return false;
        if (numero.length() == 10) numero = LADA_MX + numero;
        try {
            cliente.post().uri(apiUrl)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("messaging_product", "whatsapp", "to", numero, "type", "text", "text", Map.of("body", mensaje)))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            // Sin el teléfono ni el texto: son datos del cliente.
            logger.warn("El proveedor de WhatsApp no aceptó el aviso: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
