package com.flystore.presentacion.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * Rutas del catálogo de la tienda (CU-01) que se ven sin sesión. Comparten el prefijo /api/catalogo
 * con las del catálogo administrativo (CU-03: /filtros y /categorias/{id}/tallas), que siguen
 * exigiendo ADMIN o EMPLEADO.
 */
public final class CatalogoPublico {

    /** Patrones para Spring Security (solo GET). */
    public static final String[] RUTAS = {"/api/catalogo/*", "/api/catalogo/productos/*"};
    /** Rutas del catálogo administrativo dentro de /api/catalogo; se evalúan antes que las públicas. */
    public static final String[] RUTAS_ADMIN = {"/api/catalogo/filtros", "/api/catalogo/categorias/**"};

    private static final Pattern LINEA = Pattern.compile("^/api/catalogo/[^/]+/?$");
    private static final Pattern PRODUCTO = Pattern.compile("^/api/catalogo/productos/[^/]+/?$");

    private CatalogoPublico() {}

    /** ¿La petición es una consulta pública del catálogo de la tienda? */
    public static boolean esPublica(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        var ruta = request.getRequestURI().substring(request.getContextPath().length());
        if (PRODUCTO.matcher(ruta).matches()) return true;
        return LINEA.matcher(ruta).matches() && !ruta.startsWith("/api/catalogo/filtros");
    }
}
