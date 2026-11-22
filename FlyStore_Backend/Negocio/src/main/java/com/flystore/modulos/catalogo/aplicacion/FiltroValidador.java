package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.FiltrosCatalogoPeticionDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.consultas.OrdenCatalogo;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Valida y limpia los parámetros del catálogo de la tienda (CU-01) antes de consultar.
 * Un texto en blanco no es error: significa ver la línea completa (A1 lo resuelve la vista).
 */
@Component
public class FiltroValidador {

    static final int MAX_TEXTO = 100;
    static final int MAX_TERMINOS = 6;
    static final int MAX_OPCIONES = 50;
    static final int MAX_PAGINA = 1000;

    public int validarPagina(Integer pagina) {
        int valor = pagina == null ? 0 : pagina;
        if (valor < 0 || valor > MAX_PAGINA) throw invalido("La página debe estar entre 0 y " + MAX_PAGINA);
        return valor;
    }

    public OrdenCatalogo validarOrden(String orden) {
        if (orden == null || orden.isBlank()) return OrdenCatalogo.RECIENTES;
        try {
            return OrdenCatalogo.valueOf(orden.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalido("Orden no válido. Usa RECIENTES, PRECIO_ASC, PRECIO_DESC, NOMBRE_ASC o MAS_VENDIDOS");
        }
    }

    /** Palabras de la búsqueda en minúsculas y sin acentos, sin repetir; vacía si el texto está en blanco. */
    public List<String> terminos(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        if (texto.length() > MAX_TEXTO) throw invalido("La búsqueda admite hasta " + MAX_TEXTO + " caracteres");
        var palabras = new LinkedHashSet<String>();
        for (var palabra : normalizar(texto).split("\\s+")) {
            if (palabra.chars().anyMatch(Character::isLetterOrDigit)) palabras.add(palabra);
        }
        return new ArrayList<>(palabras).subList(0, Math.min(palabras.size(), MAX_TERMINOS));
    }

    public Set<Long> ids(List<Long> valores, String filtro) {
        if (valores == null || valores.isEmpty()) return Set.of();
        if (valores.size() > MAX_OPCIONES) throw invalido("Demasiados valores en el filtro de " + filtro);
        var ids = new LinkedHashSet<Long>();
        for (var valor : valores) {
            if (valor == null) continue;
            if (valor <= 0) throw invalido("Valor no válido en el filtro de " + filtro);
            ids.add(valor);
        }
        return ids;
    }

    public Set<String> unidades(List<String> valores) {
        if (valores == null || valores.isEmpty()) return Set.of();
        if (valores.size() > MAX_OPCIONES) throw invalido("Demasiados valores en el filtro de unidad de medida");
        var unidades = new LinkedHashSet<String>();
        for (var valor : valores) {
            if (valor == null || valor.isBlank()) continue;
            if (valor.trim().length() > 30) throw invalido("Unidad de medida no válida");
            unidades.add(valor.trim().toLowerCase(Locale.ROOT));
        }
        return unidades;
    }

    public void validarPrecios(BigDecimal minimo, BigDecimal maximo) {
        if (minimo != null && minimo.signum() < 0) throw invalido("El precio mínimo no puede ser negativo");
        if (maximo != null && maximo.signum() < 0) throw invalido("El precio máximo no puede ser negativo");
        if (minimo != null && maximo != null && minimo.compareTo(maximo) > 0)
            throw invalido("El precio mínimo no puede ser mayor que el máximo");
    }

    /** Valida todo de una vez, para rechazar la petición antes de tocar la base. */
    public void validar(FiltrosCatalogoPeticionDTO filtros) {
        validarPagina(filtros.pagina());
        validarOrden(filtros.orden());
        terminos(filtros.texto());
        ids(filtros.marcas(), "marca");
        ids(filtros.subcategorias(), "subcategoría");
        ids(filtros.tallas(), "talla");
        ids(filtros.colores(), "color");
        unidades(filtros.unidadesMedida());
        validarPrecios(filtros.precioMin(), filtros.precioMax());
    }

    /** Minúsculas, sin acentos (á → a, ñ → n) y solo letras, números y separadores simples. */
    static String normalizar(String texto) {
        var sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9./\\-\\s]", " ").trim();
    }

    private static NegocioException invalido(String mensaje) {
        return new NegocioException(TipoError.DATOS_INVALIDOS, mensaje);
    }
}
