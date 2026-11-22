package com.flystore.modulos.catalogo;

import com.flystore.contratos.catalogo.FiltrosCatalogoPeticionDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.catalogo.aplicacion.CatalogoBO;
import com.flystore.modulos.catalogo.aplicacion.EvaluadorStock;
import com.flystore.modulos.catalogo.aplicacion.FiltroValidador;
import com.flystore.modulos.catalogo.aplicacion.mappers.CatalogoBOMapper;
import com.flystore.modulos.catalogo.aplicacion.mappers.CatalogoMapper;
import com.flystore.persistencia.consultas.*;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.persistencia.entidades.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

/** CU-01 Consultar catálogo: reglas del BO con DAO en memoria. */
class ConsultarCatalogoBOTest {

    private static final UUID CLIENTE_MAYOREO = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();

    private final List<FiltroCatalogo> consultas = new ArrayList<>();
    private final List<Integer> desdes = new ArrayList<>();
    private final Map<Long, Producto> productos = new HashMap<>();
    private final Deque<PaginaProductos> paginas = new ArrayDeque<>();
    private Optional<Producto> visible = Optional.empty();
    private boolean consultoBase;

    // ===== Línea, precio por tipo de cliente y página =====

    @Test void lineaPorRutaSinImportarMayusculasYPrecioDelTipoDeCliente() {
        productos.put(4L, gorra(4L));
        paginas.add(new PaginaProductos(List.of(4L), 31));

        var respuesta = bo().consultarLinea("CaPs", peticion(null, null, 1), CLIENTE_MAYOREO);

        assertEquals("CAPS", respuesta.linea().nombre());
        assertEquals("caps", respuesta.linea().ruta());
        assertEquals("MAYOREO", respuesta.tipoPrecioReferencia());
        var tarjeta = respuesta.productos().getFirst();
        assertEquals("MAYOREO", tarjeta.precio().tipoPrecio());
        assertEquals(new BigDecimal("130.00"), tarjeta.precio().precio());
        assertEquals(6, tarjeta.precio().minPiezas());
        assertEquals("https://img/principal.jpg", tarjeta.imagenUrl());
        assertEquals(TipoPrecio.MAYOREO, consultas.getFirst().tipoPrecio());
        assertEquals(2L, consultas.getFirst().idCategoria());
        assertEquals(30, desdes.getFirst(), "la página 1 empieza en el producto 30");
        assertEquals(1, respuesta.pagina());
        assertEquals(30, respuesta.tamanoPagina());
        assertFalse(respuesta.hayMas(), "31 productos: la página 1 es la última");
        assertTrue(respuesta.sugerencias().isEmpty());
    }

    @Test void sinSesionOSiNoEsClienteSeUsaBase() {
        productos.put(4L, gorra(4L));
        paginas.add(new PaginaProductos(List.of(4L), 1));
        paginas.add(new PaginaProductos(List.of(4L), 1));

        assertEquals("BASE", bo().consultarLinea("caps", null, null).productos().getFirst().precio().tipoPrecio());
        assertEquals("BASE", bo().consultarLinea("caps", null, ADMIN).productos().getFirst().precio().tipoPrecio());
    }

    @Test void siElProductoNoTieneElEsquemaDelClienteSeMuestraBase() {
        var gorra = gorra(4L);
        gorra.getPrecios().removeIf(p -> p.getTipoPrecio() == TipoPrecio.MAYOREO);
        productos.put(4L, gorra);
        paginas.add(new PaginaProductos(List.of(4L), 1));

        var precio = bo().consultarLinea("caps", null, CLIENTE_MAYOREO).productos().getFirst().precio();
        assertEquals("BASE", precio.tipoPrecio());
        assertEquals(new BigDecimal("150.00"), precio.precio());
    }

    @Test void lineaInexistenteEsNoEncontrado() {
        var error = assertThrows(NegocioException.class, () -> bo().consultarLinea("relojes", null, null));
        assertEquals(TipoError.NO_ENCONTRADO, error.getTipoError());
    }

    @Test void filtrosSeLimpianYPasanAlDao() {
        paginas.add(new PaginaProductos(List.of(), 0));
        var peticion = new FiltrosCatalogoPeticionDTO("  Clásica  NEGRA clásica ", List.of(3L, 3L), List.of(5L), List.of(7L),
                List.of(9L), List.of(" IN "), new BigDecimal("100"), new BigDecimal("300"), "precio_asc", 0);

        bo().consultarLinea("caps", peticion, null);

        var filtro = consultas.getFirst();
        assertEquals(List.of("clasica", "negra"), filtro.terminos());
        assertEquals(Set.of(3L), filtro.marcas());
        assertEquals(Set.of(5L), filtro.subcategorias());
        assertEquals(Set.of(7L), filtro.tallas());
        assertEquals(Set.of(9L), filtro.colores());
        assertEquals(Set.of("in"), filtro.unidadesMedida());
        assertEquals(OrdenCatalogo.PRECIO_ASC, filtro.orden());
        assertEquals(new BigDecimal("100"), filtro.precioMinimo());
        assertFalse(filtro.cualquierTermino());
    }

    @Test void parametrosInvalidosSeRechazanSinConsultar() {
        var casos = List.of(
                new FiltrosCatalogoPeticionDTO(null, null, null, null, null, null, new BigDecimal("300"), new BigDecimal("100"), null, 0),
                new FiltrosCatalogoPeticionDTO(null, null, null, null, null, null, new BigDecimal("-1"), null, null, 0),
                new FiltrosCatalogoPeticionDTO(null, null, null, null, null, null, null, null, "BARATOS", 0),
                new FiltrosCatalogoPeticionDTO(null, null, null, null, null, null, null, null, null, -1),
                new FiltrosCatalogoPeticionDTO(null, List.of(0L), null, null, null, null, null, null, null, 0),
                new FiltrosCatalogoPeticionDTO("x".repeat(101), null, null, null, null, null, null, null, null, 0));
        for (var caso : casos) {
            var error = assertThrows(NegocioException.class, () -> bo().consultarLinea("caps", caso, null), caso.toString());
            assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError());
        }
        assertTrue(consultas.isEmpty(), "no debe consultar productos con parámetros inválidos");
        assertFalse(consultoBase, "ni siquiera la línea");
    }

    @Test void textoEnBlancoNoEsErrorYMuestraLaLinea() {
        paginas.add(new PaginaProductos(List.of(), 0));
        var respuesta = bo().consultarLinea("caps", peticion("   ", null, 0), null);
        assertTrue(consultas.getFirst().terminos().isEmpty());
        assertTrue(respuesta.sugerencias().isEmpty(), "sin búsqueda no hay sugerencias (A3 solo pide quitar filtros)");
    }

    // ===== Opciones de los filtros =====

    @Test void opcionesDeFiltroAgrupanTallasPorTipoYLasOrdenan() {
        paginas.add(new PaginaProductos(List.of(), 0));
        var respuesta = bo().consultarLinea("caps", null, null);
        var opciones = respuesta.opcionesFiltro();

        assertTrue(consultas.get(0).terminos().isEmpty());
        assertEquals("New Era", opciones.marcas().getFirst().nombre());
        assertEquals(96, opciones.marcas().getFirst().cantidad());
        assertEquals("#111111", opciones.colores().getFirst().hex());
        assertEquals(2, opciones.tiposTalla().size());
        var numerica = opciones.tiposTalla().stream().filter(t -> "Numérica".equals(t.nombre())).findFirst().orElseThrow();
        assertEquals("in", numerica.unidadMedida());
        assertEquals(List.of("6 7/8", "7", "7 1/8", "7 1/2"), numerica.tallas().stream().map(t -> t.nombre()).toList());
        assertEquals(List.of("in"), opciones.unidadesMedida());
        assertEquals(new BigDecimal("110.00"), opciones.rangoPrecio().minimo());
        assertEquals(5, opciones.ordenes().size());
        assertEquals("RECIENTES", opciones.ordenes().getFirst().id());
    }

    @Test void ordenDeTallasNumericasLetrasYResto() {
        var tallas = new ArrayList<>(List.of("Unitalla", "XL", "7 1/8", "S-M", "22.5", "M", "6 7/8", "Ajustable", "L-XL"));
        tallas.sort(CatalogoBOMapper.ORDEN_TALLAS);
        assertEquals(List.of("6 7/8", "7 1/8", "22.5", "S-M", "M", "L-XL", "XL", "Ajustable", "Unitalla"), tallas);
    }

    // ===== A2 · Búsqueda sin resultados =====

    @Test void busquedaSinResultadosSugiereCoincidenciasParciales() {
        productos.put(8L, gorra(8L));
        paginas.add(new PaginaProductos(List.of(), 0));
        paginas.add(new PaginaProductos(List.of(8L), 1));

        var respuesta = bo().consultarLinea("caps", peticion("gorra rosa neón", null, 0), null);

        assertEquals(0, respuesta.total());
        assertEquals(List.of(8L), respuesta.sugerencias().stream().map(s -> s.idProducto()).toList());
        var parcial = consultas.get(1);
        assertTrue(parcial.cualquierTermino(), "basta con una de las palabras");
        assertEquals(List.of("gorra", "rosa", "neon"), parcial.terminos());
        assertEquals(OrdenCatalogo.MAS_VENDIDOS, parcial.orden());
    }

    @Test void sinCoincidenciasParcialesSugiereLosMasVendidosDeLaLinea() {
        productos.put(9L, gorra(9L));
        paginas.add(new PaginaProductos(List.of(), 0));
        paginas.add(new PaginaProductos(List.of(9L), 1));

        var respuesta = bo().consultarLinea("caps", peticion("xyz", null, 0), null);

        assertEquals(List.of(9L), respuesta.sugerencias().stream().map(s -> s.idProducto()).toList());
        var masVendidos = consultas.get(1);
        assertTrue(masVendidos.terminos().isEmpty(), "con una sola palabra no hay coincidencia parcial: va a los más vendidos");
        assertEquals(OrdenCatalogo.MAS_VENDIDOS, masVendidos.orden());
        assertEquals(2L, masVendidos.idCategoria());
    }

    // ===== 3.2.4 Ficha del producto y su variante para el carrito =====

    @Test void fichaTraeVariantesConIdParaElCarritoYPrecioDelCliente() {
        var gorra = gorra(4L);
        var negro = color(2L, "Negro", "#111111");
        var gris = color(3L, "Gris", "#777777");
        var numerica = tipoTalla(10L, "Numérica", "in");
        gorra.setVariantes(new ArrayList<>(List.of(
                variante(40L, gorra, negro, talla(100L, "7 1/8", numerica), 12),
                variante(41L, gorra, negro, talla(101L, "7", numerica), 0),
                variante(42L, gorra, gris, talla(100L, "7 1/8", numerica), 3))));
        visible = Optional.of(gorra);

        var ficha = bo().obtenerProducto("4", CLIENTE_MAYOREO);

        assertEquals(4L, ficha.idProducto());
        assertEquals("caps", ficha.linea().ruta());
        assertEquals(List.of(42L, 41L, 40L), ficha.variantes().stream().map(v -> v.idVariante()).toList(),
                "por color (Gris, Negro) y luego por talla");
        var agotada = ficha.variantes().stream().filter(v -> v.idVariante() == 41L).findFirst().orElseThrow();
        assertFalse(agotada.disponible(), "A5: la variante sin existencia se marca agotada");
        assertEquals("in", agotada.unidadMedida());
        assertEquals("#111111", agotada.hex());
        assertEquals(15, ficha.existenciaTotal());
        assertTrue(ficha.disponible());
        assertEquals("MAYOREO", ficha.precioReferencia().tipoPrecio());
        assertEquals(6, ficha.cantidadInicial(), "empieza en el mínimo de su esquema para que el precio coincida con el carrito");
        assertEquals(List.of("BASE", "MAYOREO", "DISTRIBUIDOR"), ficha.precios().stream().map(p -> p.tipoPrecio()).toList());
        assertEquals(1, ficha.precios().stream().filter(p -> p.referencia()).count());
        assertEquals(List.of("https://img/principal.jpg", "https://img/negro.jpg"), ficha.imagenes().stream().map(i -> i.url()).toList());
        assertEquals(2L, ficha.imagenes().get(1).idColor());
    }

    @Test void fichaSinSesionEmpiezaEnUnaPiezaConPrecioBase() {
        var gorra = gorra(4L);
        gorra.setVariantes(new ArrayList<>(List.of(variante(40L, gorra, color(2L, "Negro", "#111111"), null, 5))));
        visible = Optional.of(gorra);

        var ficha = bo().obtenerProducto("4", null);
        assertEquals("BASE", ficha.precioReferencia().tipoPrecio());
        assertEquals(1, ficha.cantidadInicial());
        assertNull(ficha.variantes().getFirst().talla());
    }

    @Test void productoNoVisibleOIdInvalido() {
        var noVisible = assertThrows(NegocioException.class, () -> bo().obtenerProducto("99", null));
        assertEquals(TipoError.NO_ENCONTRADO, noVisible.getTipoError());
        var invalido = assertThrows(NegocioException.class, () -> bo().obtenerProducto("abc", null));
        assertEquals(TipoError.DATOS_INVALIDOS, invalido.getTipoError());
    }

    // ===== Dobles =====

    private CatalogoBO bo() {
        var categorias = puerto(ICategoriaDAO.class, (m, a) -> switch (m) {
            case "obtenerPorNombre" -> {
                consultoBase = true;
                yield "caps".equalsIgnoreCase((String) a[0]) ? Optional.of(caps()) : Optional.empty();
            }
            default -> List.of(caps());
        });
        var productoDAO = puerto(IProductoDAO.class, (m, a) -> switch (m) {
            case "buscarEnTienda" -> {
                consultas.add((FiltroCatalogo) a[0]);
                desdes.add((Integer) a[1]);
                assertTrue((Integer) a[2] == 30 || (Integer) a[2] == 4);
                yield paginas.isEmpty() ? new PaginaProductos(List.of(), 0) : paginas.poll();
            }
            case "obtenerParaTienda" -> {
                @SuppressWarnings("unchecked") var ids = (List<Long>) a[0];
                yield ids.stream().map(productos::get).filter(Objects::nonNull).toList();
            }
            case "obtenerOpcionesFiltro" -> {
                var alcance = (FiltroCatalogo) a[0];
                assertTrue(alcance.terminos().isEmpty() && alcance.marcas().isEmpty(), "las opciones se calculan sobre la línea completa");
                yield opciones();
            }
            case "obtenerVisibleEnTienda" -> visible.filter(p -> p.getId().equals(a[0]));
            default -> throw new AssertionError("No se esperaba " + m);
        });
        var clientes = puerto(IClienteDAO.class, (m, a) -> {
            if (!"findByAuthId".equals(m)) throw new AssertionError(m);
            if (CLIENTE_MAYOREO.equals(a[0])) {
                var cliente = new Cliente();
                cliente.setTipoCliente(TipoCliente.MAYOREO);
                return Optional.of(cliente);
            }
            return Optional.empty();
        });
        return new CatalogoBO(categorias, () -> List.of(), () -> List.of(), id -> List.of(), () -> List.of(), productoDAO, clientes,
                new CatalogoMapper(), new CatalogoBOMapper(), new FiltroValidador(), new EvaluadorStock());
    }

    private static FiltrosCatalogoPeticionDTO peticion(String texto, String orden, int pagina) {
        return new FiltrosCatalogoPeticionDTO(texto, null, null, null, null, null, null, null, orden, pagina);
    }

    private static OpcionesFiltroCatalogo opciones() {
        return new OpcionesFiltroCatalogo(
                List.of(new ConteoOpcion(1L, "New Era", 96)),
                List.of(new ConteoOpcion(5L, "9FORTY", 12)),
                List.of(new ConteoColor(2L, "Negro", "#111111", 40)),
                List.of(new ConteoTalla(103L, "7 1/2", 10L, "Numérica", "in", 3),
                        new ConteoTalla(100L, "7 1/8", 10L, "Numérica", "in", 5),
                        new ConteoTalla(90L, "Ajustable", 11L, "Talla única", null, 30),
                        new ConteoTalla(102L, "6 7/8", 10L, "Numérica", "in", 2),
                        new ConteoTalla(101L, "7", 10L, "Numérica", "in", 4)),
                new BigDecimal("110.00"), new BigDecimal("290.00"));
    }

    private static Categoria caps() {
        var categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNombre("CAPS ");
        return categoria;
    }

    /** Gorra con BASE $150 (1), MAYOREO $130 (6) y DISTRIBUIDOR $110 (12) e imagen principal y del color negro. */
    private static Producto gorra(long id) {
        var producto = conId(new Producto(), id);
        producto.setClave("NE-940-" + id);
        producto.setNombre("9FORTY LA Dodgers Negro");
        producto.setEstadoProducto(EstadoProducto.PUBLICADO);
        var marca = new Marca(); marca.setId(1L); marca.setNombre("New Era");
        var sub = new Subcategoria(); sub.setId(5L); sub.setNombre("9FORTY"); sub.setCategoria(caps());
        producto.setMarca(marca);
        producto.setSubcategoria(sub);
        producto.setPrecios(new ArrayList<>(List.of(
                precio(TipoPrecio.DISTRIBUIDOR, 12, "110.00"),
                precio(TipoPrecio.BASE, 1, "150.00"),
                precio(TipoPrecio.MAYOREO, 6, "130.00"))));
        var principal = conId(new ImagenProducto(), 1L); principal.setUrlImagen("https://img/principal.jpg"); principal.setOrden(0);
        var negra = conId(new ImagenProducto(), 2L); negra.setUrlImagen("https://img/negro.jpg"); negra.setOrden(1);
        negra.setColor(color(2L, "Negro", "#111111"));
        producto.setImagenes(new ArrayList<>(List.of(negra, principal)));
        producto.setVariantes(new ArrayList<>());
        return producto;
    }

    private static PrecioProducto precio(TipoPrecio tipo, int minPiezas, String precio) {
        var p = new PrecioProducto();
        p.setTipoPrecio(tipo);
        p.setMinPiezas(minPiezas);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }

    private static Color color(long id, String nombre, String hex) {
        var color = conId(new Color(), id);
        color.setNombreColor(nombre);
        color.setHex(hex);
        return color;
    }

    private static TipoTalla tipoTalla(long id, String nombre, String unidad) {
        var tipo = conId(new TipoTalla(), id);
        tipo.setNombre(nombre);
        tipo.setUnidadMedida(unidad);
        return tipo;
    }

    private static Talla talla(long id, String etiqueta, TipoTalla tipo) {
        var talla = conId(new Talla(), id);
        talla.setEtiqueta(etiqueta);
        asignar(talla, "tipoTalla", tipo);
        return talla;
    }

    private static VarianteProducto variante(long id, Producto producto, Color color, Talla talla, int stock) {
        var variante = conId(new VarianteProducto(), id);
        variante.setProducto(producto);
        variante.setColor(color);
        variante.setTalla(talla);
        variante.setSku("SKU-" + id);
        variante.setStock(stock);
        return variante;
    }

    private static <T> T conId(T entidad, long id) {
        asignar(entidad, "id", id);
        return entidad;
    }

    private static void asignar(Object entidad, String nombre, Object valor) {
        try {
            Class<?> tipo = entidad.getClass();
            while (tipo != null) {
                try {
                    Field campo = tipo.getDeclaredField(nombre);
                    campo.setAccessible(true);
                    campo.set(entidad, valor);
                    return;
                } catch (NoSuchFieldException e) {
                    tipo = tipo.getSuperclass();
                }
            }
            throw new IllegalArgumentException("Sin campo " + nombre);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
