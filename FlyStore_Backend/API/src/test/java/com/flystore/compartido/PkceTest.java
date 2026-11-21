package com.flystore.compartido;

import com.flystore.compartido.presentacion.Pkce;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PkceTest {
    @Test
    void retoS256ComoElEjemploDelRfc7636() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.reto("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test
    void verificadorAleatorioDe43Caracteres() {
        var a = Pkce.generarVerificador();
        assertEquals(43, a.length());
        assertTrue(a.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(a, Pkce.generarVerificador());
    }

    @Test
    void soloRutasInternasDelFrontend() {
        assertEquals("/carrito", Pkce.rutaSegura("/carrito"));
        assertEquals("/mis-pedidos?folio=FLY-000131", Pkce.rutaSegura("/mis-pedidos?folio=FLY-000131"));
        assertEquals("/", Pkce.rutaSegura("https://malicioso.com"));
        assertEquals("/", Pkce.rutaSegura("//malicioso.com"));
        assertEquals("/", Pkce.rutaSegura("/\\malicioso.com"));
        assertEquals("/", Pkce.rutaSegura(null));
    }
}
