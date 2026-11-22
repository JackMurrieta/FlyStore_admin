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
import com.flystore.persistencia.dao.interfaces.IProductoDAO;
import com.flystore.persistencia.entidades.Producto;
import com.flystore.modulos.catalogo.aplicacion.mappers.ProductoMapper;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test void altaCompletaRecorreControllerBOMapperYDAOCon201() throws Exception {
        var guardado = new AtomicReference<Producto>();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            if (m.equals("guardar")) { guardado.set((Producto)a[0]); return null; }
            throw new AssertionError(m);
        }), id -> false, new ProductoMapper(), null);
        var alta = MockMvcBuilders.standaloneSetup(new ProductoController(bo))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        alta.perform(post("/api/productos").contentType("application/json").content(ALTA))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        assertNotNull(guardado.get());
        assertEquals("P1", guardado.get().getClave());
        assertEquals(1L, guardado.get().getSubcategoria().getCategoria().getId());
        assertEquals(1, guardado.get().getVariantes().size());
        assertEquals(1, guardado.get().getPrecios().size());
        assertEquals(1, guardado.get().getImagenes().size());
    }

    @Test void datosInvalidosDan400YClaveDuplicada409SinGuardar() throws Exception {
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return true;
            throw new AssertionError("No guardar datos rechazados: " + m);
        }), id -> false, new ProductoMapper(), null);
        var alta = MockMvcBuilders.standaloneSetup(new ProductoController(bo))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        alta.perform(post("/api/productos").contentType("application/json").content(ALTA))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("RECURSO_YA_EXISTE"));
        alta.perform(post("/api/productos").contentType("application/json").content(ALTA.replace("\"minPiezas\":1", "\"minPiezas\":2")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("DATOS_INVALIDOS"));
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
    @Test void multipartSubeArchivoYGuardaUrlDelProveedor() throws Exception {
        var guardado = new AtomicReference<Producto>();
        var almacen = puerto(com.flystore.modulos.catalogo.aplicacion.puertos.IAlmacenImagenes.class, (m,a) -> {
            if (m.equals("subir")) return new com.flystore.modulos.catalogo.aplicacion.modelo.ImagenAlmacenada("catalogo/test.png", "https://example.com/storage/test.png");
            throw new AssertionError(m);
        });
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            if (m.equals("guardar")) { guardado.set((Producto)a[0]); return null; }
            throw new AssertionError(m);
        }), id -> false, new ProductoMapper(), almacen);
        var alta = MockMvcBuilders.standaloneSetup(new ProductoController(bo))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var metadata = new org.springframework.mock.web.MockMultipartFile("producto", "producto.json", "application/json",
                ALTA.replace("\"imagenes\":[\"https://example.com/gorra.jpg\"],", "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var imagen = new org.springframework.mock.web.MockMultipartFile("imagen", "foto.png", "image/png",
                new byte[]{(byte)137,80,78,71,13,10,26,10});
        alta.perform(multipart("/api/productos").file(metadata).file(imagen)).andExpect(status().isCreated());
        assertEquals("https://example.com/storage/test.png", guardado.get().getImagenes().get(0).getUrlImagen());
        alta.perform(multipart("/api/productos").file(metadata)).andExpect(status().isBadRequest());
        alta.perform(multipart("/api/productos").file(metadata).file(new org.springframework.mock.web.MockMultipartFile(
                "imagen", "foto.png", "image/png", new byte[]{1,2,3}))).andExpect(status().isBadRequest());
    }
    @Test void validacionDeCatalogoAtravesDelProxyDePersistenciaDevuelve400() throws Exception {
        var dao = puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            throw new com.flystore.persistencia.excepcion.DatosCatalogoInvalidosException("Selecciona un color existente");
        });
        var proxy = new org.springframework.aop.framework.ProxyFactory(dao);
        proxy.addAdvice(new org.springframework.dao.support.PersistenceExceptionTranslationInterceptor(new org.springframework.orm.jpa.vendor.HibernateJpaDialect()));
        var bo = new ProductoBO((IProductoDAO)proxy.getProxy(), id -> false, new ProductoMapper(), null);
        MockMvcBuilders.standaloneSetup(new ProductoController(bo)).setControllerAdvice(new GlobalExceptionHandler()).build()
            .perform(post("/api/productos").contentType("application/json").content(ALTA))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("DATOS_INVALIDOS"))
            .andExpect(jsonPath("$.message").value("Selecciona un color existente"));
    }

    @Test void bajaRecorreControllerBOConsultaVentasYDAOEnOrden() throws Exception {
        for (boolean ventas : new boolean[]{false, true}) {
            var pasos = new java.util.ArrayList<String>();
            var dao = puerto(IProductoDAO.class, (m,a) -> {
                pasos.add(m);
                if (m.equals("obtenerPorId")) return java.util.Optional.of(new Producto());
                assertEquals(9223372036854775807L, a[0]);
                if (m.equals("cambiarEstado")) assertEquals(com.flystore.persistencia.entidades.EstadoProducto.INACTIVO, a[1]);
                return null;
            });
            var bo = new ProductoBO(dao, id -> { pasos.add("existenVentasDeProducto"); return ventas; }, new ProductoMapper(), null);
            var api = MockMvcBuilders.standaloneSetup(new ProductoController(bo)).setControllerAdvice(new GlobalExceptionHandler()).build();
            api.perform(delete("/api/productos/9223372036854775807"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idProducto").value("9223372036854775807"))
                .andExpect(jsonPath("$.tipoBaja").value(ventas ? "LOGICA" : "FISICA"));
            assertEquals(List.of("obtenerPorId", "existenVentasDeProducto", ventas ? "cambiarEstado" : "eliminar"), pasos);
        }
    }

    @Test void bajaRechazadaNoDevuelveExito() throws Exception {
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("obtenerPorId")) return java.util.Optional.of(new Producto());
            throw new org.springframework.dao.DataIntegrityViolationException("Referencia existente");
        }), id -> false, new ProductoMapper(), null);
        var api = MockMvcBuilders.standaloneSetup(new ProductoController(bo)).setControllerAdvice(new GlobalExceptionHandler()).build();
        api.perform(delete("/api/productos/7")).andExpect(status().isConflict());
        api.perform(delete("/api/productos/no-es-id")).andExpect(status().isBadRequest());
    }

    @Test void paginaValidaLimitesYConservaFiltrosConIdsLong() throws Exception {
        var recibidos = new AtomicReference<Object[]>();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            assertEquals("buscarPagina", m); recibidos.set(a); return List.of();
        }), id -> false, new ProductoMapper(), null);
        var api = MockMvcBuilders.standaloneSetup(new ProductoController(bo)).setControllerAdvice(new GlobalExceptionHandler()).build();
        api.perform(get("/api/productos/buscar").param("idCategoria", "9223372036854775807").param("estado", "STOCK_BAJO").param("pagina", "2").param("tamano", "7"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.productos").isEmpty()).andExpect(jsonPath("$.pagina").value(2));
        assertArrayEquals(new Object[]{null, Long.MAX_VALUE, null, null, com.flystore.persistencia.entidades.EstadoProducto.STOCK_BAJO, 2, 7}, recibidos.get());
        api.perform(get("/api/productos").param("pagina", "0").param("tamano", "7")).andExpect(status().isOk());
        for (var tamano : new String[]{"0", "51", "-1"})
            api.perform(get("/api/productos").param("tamano", tamano)).andExpect(status().isBadRequest());
        api.perform(get("/api/productos").param("pagina", "-1")).andExpect(status().isBadRequest());
        api.perform(get("/api/productos").param("pagina", "2147483647")).andExpect(status().isBadRequest());
        api.perform(get("/api/productos/buscar").param("pagina", "0")).andExpect(status().isBadRequest());
    }
}

