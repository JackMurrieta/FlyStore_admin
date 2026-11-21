package com.flystore.compartido.presentacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE (RFC 7636) para el inicio de sesión con Google: el backend guarda el verificador en una
 * cookie HttpOnly y solo envía a Supabase el reto S256. Así el código de autorización no sirve
 * sin el verificador, aunque alguien lo intercepte.
 */
public final class Pkce {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private Pkce() {}

    /** Verificador aleatorio de 43 caracteres (32 bytes en base64url). */
    public static String generarVerificador() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return BASE64_URL.encodeToString(bytes);
    }

    /** Reto S256: base64url(SHA-256(verificador)). */
    public static String reto(String verificador) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verificador.getBytes(StandardCharsets.US_ASCII));
            return BASE64_URL.encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    /**
     * Ruta del frontend a la que se regresa después de iniciar sesión. Solo rutas internas
     * («/carrito»); cualquier otra cosa (URL externa, «//dominio») regresa al inicio.
     */
    public static String rutaSegura(String ruta) {
        if (ruta == null || ruta.isBlank() || !ruta.startsWith("/") || ruta.startsWith("//") || ruta.contains("\\")
                || ruta.length() > 200) return "/";
        return ruta;
    }
}
