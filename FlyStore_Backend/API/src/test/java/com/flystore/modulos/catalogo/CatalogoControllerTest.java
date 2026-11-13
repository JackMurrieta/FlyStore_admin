package com.flystore.modulos.catalogo;

import com.flystore.compartido.presentacion.GlobalExceptionHandler;
import com.flystore.contratos.catalogo.*;
import com.flystore.modulos.catalogo.aplicacion.*;
import com.flystore.modulos.catalogo.presentacion.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogoControllerTest {
    private MockMvc mvc;
    private Object[] filtrosRecibidos;
    private static final String ALTA = """
        {"clave":"P1","nombre":"Gorra","descripcion":"Gorra roja","idCategoria":"1","idSubcategoria":"2","idMarca":"3",
         "imagenes":["https://example.com/gorra.jpg"],"estado":"PUBLICADO","variantes":[{"talla":"M","color":"Rojo","stock":3}],
         "preciosProducto":[{"tipoPrecio":"BASE","precio":100.25,"minPiezas":1}]}
        """;
    @BeforeEach void preparar() {
        var producto = puerto(IProductoBO.class, (m,a) -> switch(m) {
            case "buscarProductos" -> { filtrosRecibidos = a; yield List.of(); }
            case "crearProducto" -> { assertEquals("1", ((CrearProductoDTO)a[0]).idCategoria()); yield null; }
            case "darDeBajaProducto" -> new ResultadoBajaDTO((String)a[0], "LOGICA");
            default -> List.of();
        });
        var catalogo = puerto(ICatalogoBO.class, (m,a) -> m.equals("obtenerFiltrosDisponibles")
                ? new FiltrosDisponiblesDTO(List.of(), List.of(), List.of()) : List.of(new TallaDTO("5", "M")));
        mvc = MockMvcBuilders.standaloneSetup(new ProductoController(producto), new CatalogoController(catalogo))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void altaDevuelve201SinCambiarLosIdentificadores() throws Exception {
        mvc.perform(post("/api/admin/productos").contentType("application/json").content(ALTA)).andExpect(status().isCreated()).andExpect(content().string(""));
    }
    @Test void validaDatosAnidadosYJsonMalformado() throws Exception {
        mvc.perform(post("/api/admin/productos").contentType("application/json").content(ALTA.replace("\"stock\":3", "\"stock\":-1"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/productos").contentType("application/json").content("{")).andExpect(status().isBadRequest());
    }
    @Test void bajaDevuelveElTipoDeOperacion() throws Exception {
        mvc.perform(delete("/api/admin/productos/7")).andExpect(status().isOk()).andExpect(jsonPath("$.idProducto").value("7")).andExpect(jsonPath("$.tipoBaja").value("LOGICA"));
    }
    @Test void consultaFiltrosTallasYBusqueda() throws Exception {
        mvc.perform(get("/api/admin/catalogo/filtros")).andExpect(status().isOk()).andExpect(jsonPath("$.categorias").isArray());
        mvc.perform(get("/api/admin/catalogo/categorias/1/tallas")).andExpect(status().isOk()).andExpect(jsonPath("$[0].idTalla").value("5"));
        mvc.perform(get("/api/admin/productos/buscar").param("idCategoria", "1")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void rutasDelDiagramaEnlazanTodosLosFiltrosSinConvertirIdsAEnteros() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/catalogo/filtros")).andExpect(status().isOk()).andExpect(jsonPath("$.categorias").isArray());
        mvc.perform(get("/api/productos/buscar").param("texto", "Gorra & roja")
                        .param("idCategoria", "9223372036854775807").param("idSubcategoria", "2")
                        .param("idMarca", "3").param("estado", "PUBLICADO"))
                .andExpect(status().isOk());
        assertArrayEquals(new Object[]{"Gorra & roja", "9223372036854775807", "2", "3", "PUBLICADO"}, filtrosRecibidos);
    }
}
