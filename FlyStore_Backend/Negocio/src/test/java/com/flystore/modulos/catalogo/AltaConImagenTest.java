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
import org.springframework.transaction.support.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

class AltaConImagenTest {
    private final ArchivoImagen png = new ArchivoImagen(new byte[]{(byte)137,80,78,71,13,10,26,10}, "image/png");
    private CrearProductoDTO dto() {
        return new CrearProductoDTO("NUEVO", "Gorra", "Gorra roja", "1", "2", "3",
                List.of(new ImagenColorDTO(null, null, 0, "Rojo")), "BORRADOR",
                List.of(new VarianteDTO("M", "Rojo", 1)), List.of(new PrecioProductoDTO("BASE", BigDecimal.TEN, 1)));
    }
    private final ImagenAlmacenada subida = new ImagenAlmacenada("catalogo/objeto.png", "https://proyecto.supabase.co/storage/v1/object/public/productos/catalogo/objeto.png");

    @Test void guardaSoloLaUrlDevueltaPorStorageYNoElContenido() {
        var guardado = new AtomicReference<Producto>();
        var acciones = new ArrayList<String>();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            acciones.add(m);
            if (m.equals("existePorClave")) return false;
            guardado.set((Producto)a[0]); return null;
        }), id -> false, new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> { acciones.add(m); return subida; }));
        bo.crearProductoConImagenes(dto(), List.of(png));
        assertEquals(List.of("existePorClave", "subir", "guardar"), acciones);
        assertEquals(subida.url(), guardado.get().getImagenes().getFirst().getUrlImagen());
    }

    @Test void rechazaArchivoVacioGrandeOConTipoFalsificadoAntesDeSubir() {
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> { throw new AssertionError(m); }), id -> false,
                new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> { throw new AssertionError(m); }));
        for (var imagen : List.of(new ArchivoImagen(new byte[0], "image/png"),
                new ArchivoImagen("<svg/>".getBytes(), "image/png"),
                new ArchivoImagen(new byte[ArchivoImagen.MAX_BYTES + 1], "image/png"))) {
            assertEquals(TipoError.DATOS_INVALIDOS, assertThrows(NegocioException.class, () -> bo.crearProductoConImagenes(dto(), List.of(imagen))).getTipoError());
        }
    }

    @Test void duplicadoNoSubeArchivos() {
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> true), id -> false, new ProductoMapper(),
                puerto(IAlmacenImagenes.class, (m,a) -> { throw new AssertionError("No subir un duplicado"); }));
        assertEquals(TipoError.RECURSO_YA_EXISTE, assertThrows(NegocioException.class, () -> bo.crearProductoConImagenes(dto(), List.of(png))).getTipoError());
    }

    @Test void falloDeGuardadoRetiraImagenYUnFalloDeLimpiezaNoOcultaElErrorPrincipal() {
        var eliminada = new AtomicBoolean();
        var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> {
            if (m.equals("existePorClave")) return false;
            throw new com.flystore.persistencia.excepcion.DatosCatalogoInvalidosException("Talla inválida");
        }), id -> false, new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> {
            if (m.equals("subir")) return subida;
            assertEquals(subida.ruta(), a[0]); eliminada.set(true); throw new IllegalStateException("Storage no disponible");
        }));
        var error = assertThrows(NegocioException.class, () -> bo.crearProductoConImagenes(dto(), List.of(png)));
        assertEquals("Talla inválida", error.getMessage()); assertTrue(eliminada.get());
    }

    @Test void rollbackPosteriorALaEscrituraLimpiaLaImagenPeroCommitLaConserva() {
        for (int estado : new int[]{TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK}) {
            var eliminadas = new AtomicInteger();
            var bo = new ProductoBO(puerto(IProductoDAO.class, (m,a) -> m.equals("existePorClave") ? false : null),
                    id -> false, new ProductoMapper(), puerto(IAlmacenImagenes.class, (m,a) -> {
                        if (m.equals("subir")) return subida;
                        eliminadas.incrementAndGet(); return null;
                    }));
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                bo.crearProductoConImagenes(dto(), List.of(png));
                assertEquals(0, eliminadas.get());
                TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(estado));
                assertEquals(estado == TransactionSynchronization.STATUS_ROLLED_BACK ? 1 : 0, eliminadas.get());
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }
}
