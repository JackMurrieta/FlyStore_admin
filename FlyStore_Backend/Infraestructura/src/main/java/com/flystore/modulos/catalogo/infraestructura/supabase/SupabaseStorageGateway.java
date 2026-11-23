package com.flystore.modulos.catalogo.infraestructura.supabase;

import com.flystore.infraestructura.config.SupabaseProperties;
import com.flystore.modulos.catalogo.aplicacion.modelo.*;
import com.flystore.modulos.catalogo.aplicacion.puertos.IAlmacenImagenes;
import com.flystore.dominio.excepcion.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Component
public class SupabaseStorageGateway implements IAlmacenImagenes {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorageGateway.class);
    /** Solo los archivos que sube el backend: catalogo/<uuid>.<extensión>. */
    private static final String RUTA_PROPIA = "catalogo/[a-f0-9-]{36}\\.(jpg|png|webp)";
    private final SupabaseProperties properties;
    private final RestClient cliente;

    @Autowired
    public SupabaseStorageGateway(SupabaseProperties properties) { this(properties, crearCliente(properties)); }

    SupabaseStorageGateway(SupabaseProperties properties, RestClient cliente) {
        this.properties = properties;
        this.cliente = cliente;
    }

    private static RestClient crearCliente(SupabaseProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); factory.setReadTimeout(30000);
        return RestClient.builder().baseUrl(properties.getUrl()).requestFactory(factory).build();
    }

    private HttpHeaders credenciales() {
        var clave = properties.getSecretKey();
        var bucket = properties.getStorageBucket();
        if (clave == null || clave.isBlank() || clave.startsWith("sb_publishable_")
                || bucket == null || !bucket.matches("[a-z0-9][a-z0-9_-]{0,62}"))
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "Configura la clave secreta y el bucket de Supabase Storage en el backend");
        var headers = new HttpHeaders();
        headers.set("apikey", clave);
        // Las claves modernas sb_secret no son JWT. service_role sí requiere Bearer.
        if (!clave.startsWith("sb_secret_")) headers.setBearerAuth(clave);
        return headers;
    }

    @Override public ImagenAlmacenada subir(ArchivoImagen archivo) {
        var headers = credenciales();
        var extension = switch (archivo.tipoContenido()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new NegocioException(TipoError.DATOS_INVALIDOS, "Formato de imagen no permitido");
        };
        var ruta = "catalogo/" + UUID.randomUUID() + extension;
        try {
            cliente.post().uri("/storage/v1/object/{bucket}/catalogo/{archivo}", properties.getStorageBucket(), ruta.substring("catalogo/".length()))
                    .headers(h -> h.addAll(headers)).contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                    .header("x-upsert", "false").body(archivo.contenido()).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            // Incluso un timeout puede ocurrir después de que Storage haya recibido el archivo.
            try { eliminar(ruta); } catch (RuntimeException limpieza) { logger.warn("No se pudo limpiar una subida fallida en {}", ruta); }
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo subir la imagen a Storage. Revisa la configuración y el bucket");
        }
        String base = properties.getUrl().replaceAll("/+$", "");
        return new ImagenAlmacenada(ruta, base + "/storage/v1/object/public/" + properties.getStorageBucket() + "/" + ruta);
    }

    @Override public void eliminar(String ruta) {
        if (ruta == null || !ruta.matches(RUTA_PROPIA)) throw new IllegalArgumentException("Ruta de imagen inválida");
        try {
            cliente.method(HttpMethod.DELETE).uri("/storage/v1/object/{bucket}", properties.getStorageBucket())
                    .headers(h -> h.addAll(credenciales())).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(ruta))).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo retirar la imagen de Storage");
        }
    }

    @Override public Optional<String> rutaDe(String url) {
        if (url == null || properties.getUrl() == null || properties.getStorageBucket() == null) return Optional.empty();
        String prefijo = properties.getUrl().replaceAll("/+$", "") + "/storage/v1/object/public/" + properties.getStorageBucket() + "/";
        if (!url.startsWith(prefijo)) return Optional.empty();
        String ruta = url.substring(prefijo.length());
        return ruta.matches(RUTA_PROPIA) ? Optional.of(ruta) : Optional.empty();
    }
}
