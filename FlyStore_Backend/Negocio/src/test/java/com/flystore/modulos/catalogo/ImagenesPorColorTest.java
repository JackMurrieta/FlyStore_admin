package com.flystore.modulos.catalogo;

import com.flystore.contratos.catalogo.*;
import com.flystore.dominio.excepcion.*;
import com.flystore.modulos.catalogo.aplicacion.ProductoBO;
import com.flystore.modulos.catalogo.aplicacion.mappers.ProductoMapper;
import com.flystore.modulos.catalogo.aplicacion.modelo.*;
import com.flystore.modulos.catalogo.aplicacion.puertos.IAlmacenImagenes;
import com.flystore.persistencia.dao.interfaces.IProductoDAO;
import com.flystore.persistencia.entidades.Producto;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

/** CU-03 B2 · Alta con varias imágenes, cada una ligada a un color de las variantes. */
class ImagenesPorColorTest {
    private final ArchivoImagen png = new ArchivoImagen(new byte[]{(byte)137,80,78,71,13,10,26,10}, "image/png");
    private final ArchivoImagen jpg = new ArchivoImagen(new byte[]{(byte)255,(byte)216,(byte)255,0}, "image/jpeg");

    private CrearProductoDTO dto(List<ImagenColorDTO> imagenes) {
        return new CrearProductoDTO("NE-940", "9FORTY", "Gorra ajustable", "1", "2", "3", imagenes, "PUBLICADO",
                List.of(new VarianteDTO("Ajustable", "Negro", 10), new VarianteDTO("7 1/8", "Negro", 2), new VarianteDTO("Ajustable", "Gris", 4)),
                List.of(new PrecioProductoDTO("BASE", new BigDecimal("250"), 1), new PrecioProductoDTO("MAYOREO", new BigDecimal("210"), 6)));
    }

    private static ImagenColorDTO archivo(int posicion, String color) { return new ImagenColorDTO(null, null, posicion, color); }

    @Test void subeCadaArchivoYLoLigaASuColorEnElOrdenDeLaLista() {
        var guardado = new AtomicReference<Producto>();
        var subidas = new AtomicInteger();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            if (m.equals("guardar")) { guardado.set((Producto) a[0]); return null; }
            throw new AssertionError(m);
        }), id -> false, new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> {
            int n = subidas.getAndIncrement();
            return new ImagenAlmacenada("catalogo/" + n + ".png", "https://cdn/" + n + ".png");
        }));

        // Los archivos llegan en otro orden que la galería: la lista manda.
        bo.crearProductoConImagenes(dto(List.of(archivo(2, " negro "), archivo(0, "Gris"), archivo(1, "Negro"))), List.of(png, jpg, png));

        var imagenes = guardado.get().getImagenes();
        assertEquals(List.of("https://cdn/2.png", "https://cdn/0.png", "https://cdn/1.png"),
                imagenes.stream().map(i -> i.getUrlImagen()).toList());
        assertEquals(List.of("negro", "Gris", "Negro"), imagenes.stream().map(i -> i.getColor().getNombreColor()).toList());
        assertEquals(List.of(0, 1, 2), imagenes.stream().map(i -> i.getOrden()).toList());
    }

    @Test void cadaColorDebeTenerImagenYCadaImagenUnColorDeLasVariantesAntesDeSubir() {
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> { throw new AssertionError("No consultar: " + m); }), id -> false,
                new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> { throw new AssertionError("No subir: " + m); }));
        var casos = Map.<String, Runnable>of(
                "Gris", () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, "Negro"))), List.of(png)),
                "Rojo", () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, "Negro"), archivo(1, "Gris"), archivo(2, "Rojo"))), List.of(png, png, png)),
                "no están", () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, "Negro"), archivo(1, "Gris"))), List.of(png, png, png)),
                "una sola vez", () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, "Negro"), archivo(0, "Gris"))), List.of(png)),
                "un solo origen", () -> bo.crearProductoConImagenes(dto(List.of(new ImagenColorDTO(null, "https://x/1.jpg", 0, "Negro"), archivo(1, "Gris"))), List.of(png, png)),
                "imágenes guardadas", () -> bo.crearProductoConImagenes(dto(List.of(new ImagenColorDTO("9", null, null, "Negro"), archivo(0, "Gris"))), List.of(png)),
                "color al que pertenece", () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, " "))), List.of(png)),
                "máximo 10", () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, "Negro"), archivo(1, "Gris"))), Collections.nCopies(11, png)));
        casos.forEach((mensaje, alta) -> {
            var error = assertThrows(NegocioException.class, alta::run, mensaje);
            assertEquals(TipoError.DATOS_INVALIDOS, error.getTipoError(), mensaje);
            assertTrue(error.getMessage().contains(mensaje), mensaje + " → " + error.getMessage());
        });
    }

    @Test void siFallaUnaSubidaRetiraLasYaSubidasYNoGuarda() {
        var retiradas = new ArrayList<String>();
        var subidas = new AtomicInteger();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            throw new AssertionError("No debe guardar: " + m);
        }), id -> false, new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> {
            if (m.equals("eliminar")) { retiradas.add((String) a[0]); return null; }
            if (subidas.getAndIncrement() == 1) throw new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE, "No se pudo subir la imagen");
            return new ImagenAlmacenada("catalogo/0.png", "https://cdn/0.png");
        }));
        var error = assertThrows(NegocioException.class,
                () -> bo.crearProductoConImagenes(dto(List.of(archivo(0, "Negro"), archivo(1, "Gris"))), List.of(png, png)));
        assertEquals(TipoError.SERVICIO_NO_DISPONIBLE, error.getTipoError());
        assertEquals(List.of("catalogo/0.png"), retiradas);
    }

    @Test void altaPorJsonUsaUrlsConColor() {
        var guardado = new AtomicReference<Producto>();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            guardado.set((Producto) a[0]); return null;
        }), id -> false, new ProductoMapper(), null);
        bo.crearProducto(dto(List.of(new ImagenColorDTO(null, "https://x/negro.jpg", null, "Negro"),
                new ImagenColorDTO(null, "https://x/gris.jpg", null, "Gris"))));
        assertEquals("Gris", guardado.get().getImagenes().get(1).getColor().getNombreColor());
        assertThrows(NegocioException.class, () -> bo.crearProducto(dto(List.of(new ImagenColorDTO(null, "ftp://x/negro.jpg", null, "Negro"),
                new ImagenColorDTO(null, "https://x/gris.jpg", null, "Gris")))));
    }
}
