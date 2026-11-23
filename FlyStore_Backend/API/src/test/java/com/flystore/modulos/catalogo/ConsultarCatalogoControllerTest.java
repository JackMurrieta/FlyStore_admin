package com.flystore.modulos.catalogo;

import com.flystore.compartido.presentacion.CookieUtils;
import com.flystore.compartido.presentacion.GlobalExceptionHandler;
import com.flystore.compartido.presentacion.SesionOpcional;
import com.flystore.contratos.catalogo.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.catalogo.aplicacion.ICatalogoBO;
import com.flystore.modulos.catalogo.presentacion.CatalogoController;
import com.flystore.modulos.catalogo.presentacion.ConsultarCatalogoController;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** CU-01: parámetros de la ruta de cada línea, ficha del producto y sesión opcional. */
class ConsultarCatalogoControllerTest {

    private static final UUID USUARIO = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private MockMvc mvc;
    private final List<Object[]> llamadas = new ArrayList<>();
    private boolean noEncontrado;

    @BeforeEach void preparar() {
        var bo = puerto(ICatalogoBO.class, (m, a) -> {
            llamadas.add(new Object[]{m, a});
            if (noEncontrado) throw new NegocioException(TipoError.NO_ENCONTRADO, "No existe la línea");
            return switch (m) {
                case "consultarLinea", "buscar" -> catalogo();
                case "obtenerProducto" -> ficha();
                case "obtenerFiltrosDisponibles" -> new FiltrosDisponiblesDTO(List.of(), List.of(), List.of());
                default -> List.of();
            };
        });
        var sesion = new SesionOpcional(new CookieUtils(), token -> {
            if (!"valido".equals(token)) throw new BadJwtException("Token vencido");
            return Jwt.withTokenValue(token).header("alg", "ES256").subject(USUARIO.toString()).build();
        });
        mvc = MockMvcBuilders.standaloneSetup(new ConsultarCatalogoController(bo, sesion), new CatalogoController(bo))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void lineaRecibeBusquedaFiltrosOrdenYPagina() throws Exception {
        mvc.perform(get("/api/catalogo/caps").param("texto", "9forty").param("marca", "1", "2").param("talla", "7,8")
                        .param("color", "9").param("unidadMedida", "in").param("precioMin", "150").param("precioMax", "300")
                        .param("orden", "PRECIO_ASC").param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linea.ruta").value("caps"))
                .andExpect(jsonPath("$.productos[0].precio.tipoPrecio").value("MAYOREO"))
                .andExpect(jsonPath("$.opcionesFiltro.tiposTalla[0].unidadMedida").value("in"))
                .andExpect(jsonPath("$.hayMas").value(true));
        var args = (Object[]) llamadas.getFirst()[1];
        assertEquals("caps", args[0]);
        var filtros = (FiltrosCatalogoPeticionDTO) args[1];
        assertEquals("9forty", filtros.texto());
        assertEquals(List.of(1L, 2L), filtros.marcas());
        assertEquals(List.of(7L, 8L), filtros.tallas());
        assertEquals(List.of(9L), filtros.colores());
        assertEquals(List.of("in"), filtros.unidadesMedida());
        assertEquals(0, new BigDecimal("150").compareTo(filtros.precioMin()));
        assertEquals("PRECIO_ASC", filtros.orden());
        assertEquals(2, filtros.pagina());
        assertNull(args[2], "sin cookie entra como visitante");
    }

    @Test void busquedaEnTodaLaTiendaNoSeConfundeConUnaLinea() throws Exception {
        mvc.perform(get("/api/catalogo/buscar").param("texto", "9forty").param("linea", "caps").param("color", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[0].colores[0].imagenUrl").value("https://img/negro.jpg"))
                .andExpect(jsonPath("$.opcionesFiltro.marcas[0].logoUrl").value("https://logos/ne.png"))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(48));
        assertEquals("buscar", llamadas.getFirst()[0]);
        var args = (Object[]) llamadas.getFirst()[1];
        assertEquals("caps", args[0]);
        assertEquals("9forty", ((FiltrosCatalogoPeticionDTO) args[1]).texto());
        assertEquals(List.of(2L), ((FiltrosCatalogoPeticionDTO) args[1]).colores());
    }

    @Test void sesionOpcionalUsaElClienteYUnTokenVencidoNoDa401() throws Exception {
        mvc.perform(get("/api/catalogo/caps").cookie(new Cookie("access_token", "valido"))).andExpect(status().isOk());
        assertEquals(USUARIO, ((Object[]) llamadas.get(0)[1])[2]);
        mvc.perform(get("/api/catalogo/caps").cookie(new Cookie("access_token", "vencido"))).andExpect(status().isOk());
        assertNull(((Object[]) llamadas.get(1)[1])[2], "con el token vencido se ve como visitante");
    }

    @Test void filtrosDelAdminTienenPrioridadSobreLaRutaDeLinea() throws Exception {
        mvc.perform(get("/api/catalogo/filtros")).andExpect(status().isOk());
        assertEquals("obtenerFiltrosDisponibles", llamadas.getFirst()[0]);
    }

    @Test void fichaDelProductoTraeLaVarianteParaElCarrito() throws Exception {
        mvc.perform(get("/api/catalogo/productos/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantes[0].idVariante").value(40))
                .andExpect(jsonPath("$.variantes[1].disponible").value(false))
                .andExpect(jsonPath("$.cantidadInicial").value(1));
        var args = (Object[]) llamadas.getFirst()[1];
        assertEquals("obtenerProducto", llamadas.getFirst()[0]);
        assertEquals("4", args[0]);
    }

    @Test void parametroMalFormadoEs400YLineaInexistente404() throws Exception {
        mvc.perform(get("/api/catalogo/caps").param("marca", "abc")).andExpect(status().isBadRequest());
        noEncontrado = true;
        mvc.perform(get("/api/catalogo/relojes")).andExpect(status().isNotFound());
    }

    private static CatalogoRespuestaDTO catalogo() {
        var precio = new PrecioReferenciaDTO("MAYOREO", new BigDecimal("130.00"), 6);
        var tarjeta = new ProductoTarjetaDTO(4L, "NE-940", "9FORTY LA Dodgers", "New Era", "9FORTY", "https://img/1.jpg", precio,
                List.of(new ColorTarjetaDTO(2L, "Negro", "#111111", "https://img/negro.jpg")), null);
        var opciones = new OpcionesFiltroRespuestaDTO(List.of(new MarcaFiltroDTO(1L, "New Era", "https://logos/ne.png", 96)), List.of(), List.of(),
                List.of(new TipoTallaFiltroDTO(10L, "Numérica", "in", List.of(new OpcionFiltroDTO(100L, "7 1/8", 5)))),
                List.of("in"), new RangoPrecioDTO(new BigDecimal("110"), new BigDecimal("290")), List.of());
        return new CatalogoRespuestaDTO(new LineaDTO(2L, "CAPS", "caps", null), List.of(tarjeta), opciones, 0, 30, 48, true,
                "MAYOREO", List.of(), List.of(new LineaConteoDTO(2L, "CAPS", "caps", 48)));
    }

    private static ProductoDetalleRespuestaDTO ficha() {
        var negro = new VarianteVentaDTO(40L, "SKU-40", 2L, "Negro", "#111111", 100L, "7 1/8", 10L, "Numérica", "in", 12, true);
        var agotada = new VarianteVentaDTO(41L, "SKU-41", 2L, "Negro", "#111111", 101L, "7", 10L, "Numérica", "in", 0, false);
        return new ProductoDetalleRespuestaDTO(4L, "NE-940", "9FORTY LA Dodgers", "Gorra", "PUBLICADO",
                new LineaDTO(2L, "CAPS", "caps", null), new ElementoCatalogoDTO(1L, "New Era"), new ElementoCatalogoDTO(5L, "9FORTY"),
                List.of(new ImagenProductoDTO("https://img/1.jpg", null, 0)), List.of(negro, agotada),
                List.of(new NivelPrecioCatalogoDTO("BASE", 1, new BigDecimal("150"), true)),
                new PrecioReferenciaDTO("BASE", new BigDecimal("150"), 1), 1, 12, true);
    }
}
