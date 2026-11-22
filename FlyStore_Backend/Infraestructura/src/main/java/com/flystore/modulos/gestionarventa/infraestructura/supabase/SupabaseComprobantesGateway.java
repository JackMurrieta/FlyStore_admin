package com.flystore.modulos.gestionarventa.infraestructura.supabase;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.infraestructura.config.SupabaseProperties;
import com.flystore.modulos.gestionarventa.aplicacion.puertos.IAlmacenComprobantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.Map;

/**
 * Comprobantes de pago en un bucket PRIVADO de Supabase Storage. A diferencia de las imágenes del
 * catálogo, nunca se genera una URL pública: solo se guarda la ruta del objeto.
 */
@Component
public class SupabaseComprobantesGateway implements IAlmacenComprobantes {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseComprobantesGateway.class);
    private static final String RUTA_VALIDA = "pedidos/\\d+/FLY-\\d{6,}/[0-9a-f-]{36}\\.(jpg|png|pdf)";

    private final SupabaseProperties properties;
    private final RestClient cliente;

    @Autowired
    public SupabaseComprobantesGateway(SupabaseProperties properties) {
        this(properties, crearCliente(properties));
    }

    SupabaseComprobantesGateway(SupabaseProperties properties, RestClient cliente) {
        this.properties = properties;
        this.cliente = cliente;
    }

    private static RestClient crearCliente(SupabaseProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        return RestClient.builder().baseUrl(properties.getUrl()).requestFactory(factory).build();
    }

    private HttpHeaders credenciales() {
        var clave = properties.getSecretKey();
        var bucket = properties.getComprobantesBucket();
        if (clave == null || clave.isBlank() || clave.startsWith("sb_publishable_")
                || bucket == null || !bucket.matches("[a-z0-9][a-z0-9_-]{0,62}"))
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo guardar el comprobante. Intenta más tarde.");
        var headers = new HttpHeaders();
        headers.set("apikey", clave);
        // Las claves sb_secret no son JWT; service_role sí requiere Bearer.
        if (!clave.startsWith("sb_secret_")) headers.setBearerAuth(clave);
        return headers;
    }

    @Override
    public String subir(String ruta, byte[] contenido, String tipoContenido) {
        validarRuta(ruta);
        var headers = credenciales();
        // Un segmento por variable: RestClient codificaría las barras de una sola variable como %2F.
        var segmentos = ruta.split("/");
        try {
            cliente.post().uri("/storage/v1/object/{bucket}/pedidos/{cliente}/{folio}/{archivo}",
                            properties.getComprobantesBucket(), segmentos[1], segmentos[2], segmentos[3])
                    .headers(h -> h.addAll(headers)).contentType(MediaType.parseMediaType(tipoContenido))
                    .header("x-upsert", "false").body(contenido).retrieve().toBodilessEntity();
            return ruta;
        } catch (RestClientException e) {
            // Un timeout puede ocurrir después de que Storage guardó el archivo: se intenta limpiar.
            try { eliminar(ruta); } catch (RuntimeException limpieza) { logger.warn("No se pudo limpiar una subida fallida en {}", ruta); }
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo guardar el comprobante. Intenta de nuevo.");
        }
    }

    @Override
    public void eliminar(String ruta) {
        validarRuta(ruta);
        try {
            cliente.method(HttpMethod.DELETE).uri("/storage/v1/object/{bucket}", properties.getComprobantesBucket())
                    .headers(h -> h.addAll(credenciales())).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(ruta))).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo retirar el comprobante de Storage");
        }
    }

    @Override
    public String generarUrlTemporal(String ruta, int segundos) {
        validarRuta(ruta);
        var segmentos = ruta.split("/");
        try {
            Map<?, ?> respuesta = cliente.post()
                    .uri("/storage/v1/object/sign/{bucket}/pedidos/{cliente}/{folio}/{archivo}",
                            properties.getComprobantesBucket(), segmentos[1], segmentos[2], segmentos[3])
                    .headers(h -> h.addAll(credenciales())).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", segundos)).retrieve().body(Map.class);
            var firmada = respuesta == null ? null : respuesta.get("signedURL");
            if (!(firmada instanceof String relativa) || relativa.isBlank())
                throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo generar el enlace del comprobante");
            // Storage responde la ruta relativa a /storage/v1 (por ejemplo /object/sign/...?token=...).
            return properties.getUrl().replaceAll("/+$", "") + "/storage/v1" + (relativa.startsWith("/") ? relativa : "/" + relativa);
        } catch (RestClientException e) {
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo generar el enlace del comprobante");
        }
    }

    private static void validarRuta(String ruta) {
        if (ruta == null || !ruta.matches(RUTA_VALIDA)) throw new IllegalArgumentException("Ruta de comprobante inválida");
    }
}
