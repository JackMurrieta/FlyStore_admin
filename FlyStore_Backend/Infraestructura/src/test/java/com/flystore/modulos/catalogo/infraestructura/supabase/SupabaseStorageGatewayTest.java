package com.flystore.modulos.catalogo.infraestructura.supabase;

import com.flystore.infraestructura.config.SupabaseProperties;
import com.flystore.modulos.catalogo.aplicacion.modelo.ArchivoImagen;
import com.flystore.dominio.excepcion.NegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SupabaseStorageGatewayTest {
    private SupabaseProperties properties(String key) {
        var props = new SupabaseProperties();
        props.setUrl("https://proyecto.supabase.co"); props.setSecretKey(key); props.setStorageBucket("productos");
        return props;
    }
    @Test void subeBytesConClaveSecretaYRetornaUrlPublicaLuegoEliminaSoloEseObjeto() {
        var builder = RestClient.builder().baseUrl("https://proyecto.supabase.co");
        var server = MockRestServiceServer.bindTo(builder).build();
        var ruta = new AtomicReference<String>();
        byte[] datos = { (byte)137,80,78,71,13,10,26,10 };
        server.expect(request -> {
            String path = request.getURI().getPath();
            assertTrue(path.matches("/storage/v1/object/productos/catalogo/[a-f0-9-]{36}\\.png"));
            ruta.set(path.substring("/storage/v1/object/productos/".length()));
        }).andExpect(method(HttpMethod.POST)).andExpect(header("apikey", "sb_secret_prueba"))
                .andExpect(headerDoesNotExist("Authorization")).andExpect(header("x-upsert", "false"))
                .andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(datos))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://proyecto.supabase.co/storage/v1/object/productos"))
                .andExpect(method(HttpMethod.DELETE)).andExpect(jsonPath("$.prefixes[0]", org.hamcrest.Matchers.startsWith("catalogo/")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var adapter = new SupabaseStorageGateway(properties("sb_secret_prueba"), builder.build());
        var imagen = adapter.subir(new ArchivoImagen(datos, "image/png"));
        assertEquals(ruta.get(), imagen.ruta());
        assertEquals("https://proyecto.supabase.co/storage/v1/object/public/productos/" + ruta.get(), imagen.url());
        adapter.eliminar(imagen.ruta());
        server.verify();
    }

    @Test void errorDeStorageIntentaLimpiarLaRutaSinExponerCredenciales() {
        var builder = RestClient.builder().baseUrl("https://proyecto.supabase.co");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(method(HttpMethod.POST)).andRespond(withServerError());
        server.expect(method(HttpMethod.DELETE)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var adapter = new SupabaseStorageGateway(properties("sb_secret_prueba"), builder.build());
        var error = assertThrows(NegocioException.class, () -> adapter.subir(new ArchivoImagen(new byte[]{1}, "image/jpeg")));
        assertFalse(error.getMessage().contains("sb_secret"));
        server.verify();
    }

    @Test void admiteClaveServiceRoleAnteriorEnElBackend() {
        var builder = RestClient.builder().baseUrl("https://proyecto.supabase.co");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(method(HttpMethod.POST)).andExpect(header("apikey", "eyJ.prueba.service_role"))
                .andExpect(header("Authorization", "Bearer eyJ.prueba.service_role"))
                .andRespond(withSuccess());
        new SupabaseStorageGateway(properties("eyJ.prueba.service_role"), builder.build())
                .subir(new ArchivoImagen(new byte[]{1}, "image/webp"));
        server.verify();
    }

    @Test void rechazaClavePublicaYNoPermiteEliminarRutasAjenas() {
        var builder = RestClient.builder().baseUrl("https://proyecto.supabase.co");
        var server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new SupabaseStorageGateway(properties("sb_publishable_prueba"), builder.build());
        assertThrows(NegocioException.class, () -> adapter.subir(new ArchivoImagen(new byte[]{1}, "image/png")));
        assertThrows(IllegalArgumentException.class, () -> adapter.eliminar("../otra/ruta.png"));
        server.verify();
    }
}
