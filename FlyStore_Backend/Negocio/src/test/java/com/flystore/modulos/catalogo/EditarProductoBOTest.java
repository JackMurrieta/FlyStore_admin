package com.flystore.modulos.catalogo;

import com.flystore.contratos.catalogo.*;
import com.flystore.dominio.excepcion.*;
import com.flystore.modulos.catalogo.aplicacion.ProductoBO;
import com.flystore.modulos.catalogo.aplicacion.mappers.ProductoMapper;
import com.flystore.modulos.catalogo.aplicacion.modelo.*;
import com.flystore.modulos.catalogo.aplicacion.puertos.IAlmacenImagenes;
import com.flystore.persistencia.cambios.CambiosProducto;
import com.flystore.persistencia.dao.interfaces.IProductoDAO;
import com.flystore.persistencia.entidades.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

/** CU-03 S1 · Editar producto: datos, variantes, imágenes por color y precios. */
class EditarProductoBOTest {
    private static final String PROPIA = "https://proyecto.supabase.co/storage/v1/object/public/productos/catalogo/gris.png";
    private static final String EXTERNA = "https://proyecto.supabase.co/storage/v1/object/public/productos/flycaps/blanco/01.webp";
    private final ArchivoImagen png = new ArchivoImagen(new byte[]{(byte)137,80,78,71,13,10,26,10}, "image/png");

    private final Producto producto = producto();
    private final AtomicReference<CambiosProducto> guardado = new AtomicReference<>();
    private final List<String> eliminadas = new ArrayList<>();
    private final Set<String> enUso = new HashSet<>();
    private final List<String> llamadas = new ArrayList<>();

    // ===== Cargar el formulario =====

    @Test void cargaElProductoConIdsColoresYVariantesConPedidos() {
        var dto = bo().obtenerProductoParaEditar("7");
        assertEquals("NE-940", dto.clave());
        assertEquals("1", dto.idCategoria());
        assertEquals("2", dto.idSubcategoria());
        assertEquals(List.of("40", "41", "42"), dto.variantes().stream().map(VarianteAdminDTO::idVariante).toList());
        assertEquals(List.of(false, true, false), dto.variantes().stream().map(VarianteAdminDTO::conPedidos).toList());
        assertEquals(List.of("Negro", "Gris", "Blanco"), dto.imagenes().stream().map(ImagenAdminDTO::color).toList());
        assertEquals(List.of("55", "56", "57"), dto.imagenes().stream().map(ImagenAdminDTO::idImagen).toList());
        assertEquals(List.of(1, 6), dto.preciosProducto().stream().map(PrecioProductoDTO::minPiezas).toList());
        assertThrows(NegocioException.class, () -> bo().obtenerProductoParaEditar("x"));
    }

    // ===== Guardar =====

    @Test void guardaExistenciasVariantesNuevasImagenesPorColorYPrecios() {
        bo().editarProducto("7", edicion(), List.of(png));

        var cambios = guardado.get();
        assertEquals("NE-940-B", cambios.clave());
        assertEquals(EstadoProducto.BORRADOR, cambios.estado());
        assertEquals(Map.of(40L, 7, 41L, 0), cambios.existencias(),
                "40 cambia su existencia, 41 tiene pedidos y se queda en 0, 42 se borra");
        assertEquals(1, cambios.variantesNuevas().size());
        assertEquals("Rojo", cambios.variantesNuevas().getFirst().getColor().getNombreColor());
        assertEquals(List.of(new CambiosProducto.ImagenCambio(55L, null, "Negro"),
                new CambiosProducto.ImagenCambio(null, "https://cdn/nueva.png", "Rojo")), cambios.imagenes());
        assertEquals(List.of(new BigDecimal("260"), new BigDecimal("220")), cambios.precios().stream().map(PrecioProducto::getPrecio).toList());
        assertEquals(List.of("catalogo/gris.png"), eliminadas,
                "solo se borra de Storage la imagen quitada que subió el backend; la de otra carpeta se conserva");
    }

    @Test void imagenQuitadaQueUsaOtroProductoNoSeBorraDeStorage() {
        enUso.add(PROPIA);
        bo().editarProducto("7", edicion(), List.of(png));
        assertEquals(List.of(), eliminadas);
    }

    @Test void conTransaccionBorraAlConfirmarYSiSeRevierteRetiraSoloLoSubido() {
        for (int estado : new int[]{TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK}) {
            eliminadas.clear();
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                bo().editarProducto("7", edicion(), List.of(png));
                assertEquals(List.of(), eliminadas, "nada se borra antes de terminar la transacción");
                for (var s : TransactionSynchronizationManager.getSynchronizations()) {
                    if (estado == TransactionSynchronization.STATUS_COMMITTED) s.afterCommit();
                    s.afterCompletion(estado);
                }
                assertEquals(estado == TransactionSynchronization.STATUS_COMMITTED ? List.of("catalogo/gris.png") : List.of("catalogo/nueva.png"),
                        eliminadas);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }

    @Test void validaLaPeticionAntesDeConsultarLaBase() {
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> { throw new AssertionError("No consultar: " + m); }), id -> false,
                new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> { throw new AssertionError("No subir: " + m); }));
        var base = edicion();
        for (var dto : List.of(
                con(base, "estado", "AGOTADO"),
                con(base, "clave", " "),
                con(base, "variantes", List.of(new VarianteEdicionDTO("40", null, null, -1))),
                con(base, "variantes", List.of(new VarianteEdicionDTO(null, "Ajustable", "Rojo", 1), new VarianteEdicionDTO(null, "ajustable", "ROJO", 2))),
                con(base, "preciosProducto", List.of(new PrecioProductoDTO("MAYOREO", BigDecimal.TEN, 6))),
                con(base, "imagenes", List.of(new ImagenColorDTO("55", null, 0, "Negro"))))) {
            assertEquals(TipoError.DATOS_INVALIDOS, assertThrows(NegocioException.class, () -> bo.editarProducto("7", dto, List.of(png))).getTipoError());
        }
    }

    @Test void reglasQueDependenDelProductoGuardado() {
        var casos = new LinkedHashMap<String, EditarProductoDTO>();
        var base = edicion();
        casos.put("no pertenece", con(base, "variantes", List.of(new VarianteEdicionDTO("99", null, null, 1), nuevaRoja())));
        casos.put("quita la variante", con(base, "variantes", List.of(new VarianteEdicionDTO("40", "Ajustable", "Gris", 1), nuevaRoja())));
        casos.put("ya existe", con(base, "variantes", List.of(new VarianteEdicionDTO("40", null, null, 1), new VarianteEdicionDTO(null, " ajustable ", "negro", 1), nuevaRoja())));
        casos.put("Elige Publicado", con(base, "estado", "STOCK_BAJO"));
        casos.put("del color «Rojo»", con(base, "imagenes", List.of(new ImagenColorDTO("55", null, null, "Negro"), new ImagenColorDTO(null, null, 0, "Negro"))));
        casos.put("«Gris» no corresponde", con(base, "imagenes", List.of(new ImagenColorDTO("55", null, null, "Negro"),
                new ImagenColorDTO("56", null, null, "Gris"), new ImagenColorDTO(null, null, 0, "Rojo"))));
        casos.put("imagen 99 no pertenece", con(base, "imagenes", List.of(new ImagenColorDTO("99", null, null, "Negro"),
                new ImagenColorDTO(null, null, 0, "Rojo"))));
        casos.forEach((mensaje, dto) -> {
            var error = assertThrows(NegocioException.class, () -> bo().editarProducto("7", dto, List.of(png)), mensaje);
            assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError(), mensaje);
            assertTrue(error.getMessage().contains(mensaje), mensaje + " → " + error.getMessage());
            assertNull(guardado.get(), "no se guarda: " + mensaje);
        });
    }

    @Test void varianteQuitadaSinPedidosSePuedeVolverAAgregar() {
        // 40 (Negro/Ajustable) se quita y no tiene pedidos: agregar Negro/Ajustable nueva no es repetida.
        bo().editarProducto("7", con(edicion(), "variantes", List.of(new VarianteEdicionDTO(null, "Ajustable", "Negro", 3), nuevaRoja())), List.of(png));
        assertEquals(Map.of(41L, 0), guardado.get().existencias());
        assertEquals(2, guardado.get().variantesNuevas().size());
    }

    @Test void claveDeOtroProductoYProductoInexistente() {
        var duplicada = assertThrows(NegocioException.class, () -> bo(true).editarProducto("7", edicion(), List.of(png)));
        assertEquals(TipoError.RECURSO_YA_EXISTE, duplicada.getTipoError());
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> Optional.empty()), id -> false, new ProductoMapper(), null);
        assertEquals(TipoError.RECURSO_NO_ENCONTRADO, assertThrows(NegocioException.class, () -> bo.editarProducto("7", edicion(), List.of(png))).getTipoError());
        assertEquals(TipoError.RECURSO_NO_ENCONTRADO, assertThrows(NegocioException.class, () -> bo.obtenerProductoParaEditar("7")).getTipoError());
    }

    @Test void mismaClaveConOtrasMayusculasNoConsultaDuplicados() {
        bo(true).editarProducto("7", con(edicion(), "clave", " ne-940 "), List.of(png));
        assertFalse(llamadas.contains("existeOtroConClave"));
        assertEquals("ne-940", guardado.get().clave());
    }

    // ===== Datos =====

    /** Negro/Ajustable 40 (10), Gris/Ajustable 41 (4, con pedidos), Blanco/Ajustable 42 (0); fotos 55 negro, 56 gris (propia), 57 blanco (otra carpeta). */
    private static Producto producto() {
        var categoria = new Categoria(); categoria.setId(1L); categoria.setNombre("CAPS");
        var sub = new Subcategoria(); sub.setId(2L); sub.setNombre("9FORTY"); sub.setCategoria(categoria);
        var marca = new Marca(); marca.setId(3L); marca.setNombre("New Era");
        var p = conId(new Producto(), 7L);
        p.setClave("NE-940"); p.setNombre("9FORTY LA Dodgers"); p.setDescripcion("Gorra"); p.setEstadoProducto(EstadoProducto.PUBLICADO);
        p.setSubcategoria(sub); p.setMarca(marca);
        var ajustable = conId(new Talla(), 25L); ajustable.setEtiqueta("Ajustable");
        var negro = color(2L, "Negro"); var gris = color(10L, "Gris"); var blanco = color(11L, "Blanco");
        p.setVariantes(new ArrayList<>(List.of(variante(42L, blanco, ajustable, 0), variante(40L, negro, ajustable, 10), variante(41L, gris, ajustable, 4))));
        p.setImagenes(new ArrayList<>(List.of(imagen(57L, EXTERNA, blanco, 2), imagen(55L, "https://cdn/negro.png", negro, 0), imagen(56L, PROPIA, gris, 1))));
        var base = new PrecioProducto(); base.setTipoPrecio(TipoPrecio.BASE); base.setMinPiezas(1); base.setPrecio(new BigDecimal("250"));
        var mayoreo = new PrecioProducto(); mayoreo.setTipoPrecio(TipoPrecio.MAYOREO); mayoreo.setMinPiezas(6); mayoreo.setPrecio(new BigDecimal("210"));
        p.setPrecios(new ArrayList<>(List.of(mayoreo, base)));
        return p;
    }

    /** Conserva Negro (40, existencia 7) y su foto, agrega Rojo con una foto nueva; quita Gris (tiene pedidos) y Blanco. */
    private static EditarProductoDTO edicion() {
        return new EditarProductoDTO("NE-940-B", "9FORTY LA Dodgers", "Gorra ajustable", "2", "3", "BORRADOR",
                List.of(new VarianteEdicionDTO("40", null, null, 7), nuevaRoja()),
                List.of(new PrecioProductoDTO("BASE", new BigDecimal("260"), 1), new PrecioProductoDTO("MAYOREO", new BigDecimal("220"), 6)),
                List.of(new ImagenColorDTO("55", null, null, "Negro"), new ImagenColorDTO(null, null, 0, "Rojo")));
    }

    private static VarianteEdicionDTO nuevaRoja() { return new VarianteEdicionDTO(null, "Ajustable", "Rojo", 5); }

    private ProductoBO bo() { return bo(false); }

    private ProductoBO bo(boolean claveOcupada) {
        var dao = puerto(IProductoDAO.class, (m,a) -> {
            llamadas.add(m);
            return switch (m) {
                case "obtenerPorId", "obtenerParaEditar" -> Optional.of(producto);
                case "variantesConPedidos" -> Set.of(41L);
                case "existeOtroConClave" -> claveOcupada;
                case "actualizar" -> { guardado.set((CambiosProducto) a[1]); yield null; }
                case "imagenEnUso" -> enUso.contains((String) a[0]);
                default -> throw new AssertionError(m);
            };
        });
        var almacen = puerto(IAlmacenImagenes.class, (m,a) -> switch (m) {
            case "subir" -> new ImagenAlmacenada("catalogo/nueva.png", "https://cdn/nueva.png");
            case "rutaDe" -> ((String) a[0]).contains("/catalogo/") ? Optional.of("catalogo/" + ((String) a[0]).substring(((String) a[0]).lastIndexOf('/') + 1)) : Optional.empty();
            case "eliminar" -> { eliminadas.add((String) a[0]); yield null; }
            default -> throw new AssertionError(m);
        });
        return new ProductoBO(dao, id -> true, new ProductoMapper(), almacen);
    }

    /** Copia del DTO con un campo cambiado. */
    private static EditarProductoDTO con(EditarProductoDTO dto, String campo, Object valor) {
        var valores = new LinkedHashMap<String, Object>();
        valores.put("clave", dto.clave()); valores.put("nombre", dto.nombre()); valores.put("descripcion", dto.descripcion());
        valores.put("idSubcategoria", dto.idSubcategoria()); valores.put("idMarca", dto.idMarca()); valores.put("estado", dto.estado());
        valores.put("variantes", dto.variantes()); valores.put("preciosProducto", dto.preciosProducto()); valores.put("imagenes", dto.imagenes());
        valores.put(campo, valor);
        @SuppressWarnings("unchecked") var variantes = (List<VarianteEdicionDTO>) valores.get("variantes");
        @SuppressWarnings("unchecked") var precios = (List<PrecioProductoDTO>) valores.get("preciosProducto");
        @SuppressWarnings("unchecked") var imagenes = (List<ImagenColorDTO>) valores.get("imagenes");
        return new EditarProductoDTO((String) valores.get("clave"), (String) valores.get("nombre"), (String) valores.get("descripcion"),
                (String) valores.get("idSubcategoria"), (String) valores.get("idMarca"), (String) valores.get("estado"), variantes, precios, imagenes);
    }

    private static Color color(long id, String nombre) { var c = conId(new Color(), id); c.setNombreColor(nombre); return c; }

    private static VarianteProducto variante(long id, Color color, Talla talla, int stock) {
        var v = conId(new VarianteProducto(), id); v.setColor(color); v.setTalla(talla); v.setStock(stock); v.setSku("SKU-" + id); return v;
    }

    private static ImagenProducto imagen(long id, String url, Color color, int orden) {
        var i = conId(new ImagenProducto(), id); i.setUrlImagen(url); i.setColor(color); i.setOrden(orden); return i;
    }

    private static <T> T conId(T entidad, long id) {
        try {
            Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
            return entidad;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
