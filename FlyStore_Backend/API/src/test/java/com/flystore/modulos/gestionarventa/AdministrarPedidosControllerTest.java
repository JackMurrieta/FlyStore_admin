package com.flystore.modulos.gestionarventa;

import com.flystore.compartido.presentacion.GlobalExceptionHandler;
import com.flystore.contratos.administrarpedidos.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.gestionarventa.aplicacion.IPedidoVentaBO;
import com.flystore.modulos.gestionarventa.aplicacion.modelo.DatosEnvio;
import com.flystore.modulos.gestionarventa.presentacion.AdministrarPedidosController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contrato HTTP de CU-04: rutas, JSON en snake_case (como lo envía y espera el frontend), quién
 * ejecuta la acción tomado de la sesión y traducción de los errores de negocio. La autorización
 * de /api/admin/** se prueba en SecurityConfigTest.
 */
class AdministrarPedidosControllerTest {
    private static final String ADMIN = "11111111-1111-1111-1111-111111111111";
    private static final TestingAuthenticationToken SESION = new TestingAuthenticationToken(ADMIN, null);

    private final List<Object[]> llamadas = new ArrayList<>();
    private NegocioException error;

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdministrarPedidosController(
                    puerto(IPedidoVentaBO.class, (metodo, argumentos) -> {
                        var llamada = new ArrayList<Object>(List.of(metodo));
                        llamada.addAll(Arrays.asList(argumentos));
                        llamadas.add(llamada.toArray());
                        if (error != null) throw error;
                        return switch (metodo) {
                            case "buscarPedidos" -> new PaginaPedidosDTO(List.of(resumen()), 1);
                            case "obtenerDetallePedido" -> detalle();
                            default -> new ResultadoAccionDTO(detalle(), false);
                        };
                    })))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    private static PedidoResumenDTO resumen() {
        return new PedidoResumenDTO("FLY-000128", "2026-11-20T08:00:00-07:00", "Ana Torres", "644 123 4567",
                new BigDecimal("420.00"), "TRANSFERENCIA", "PAGO_EN_REVISION", null);
    }

    private static DetallePedidoAdminDTO detalle() {
        return new DetallePedidoAdminDTO("FLY-000128", "ENVIADO", "2026-11-20T08:00:00-07:00", null,
                new ClienteDTO(7L, "Ana Torres", "644 123 4567"), "Calle Uno 123, Ciudad Obregón, Sonora",
                List.of(new LineaPedidoDTO("9FORTY LA Dodgers · Negro", "Ajustable", 2, new BigDecimal("210.00"), new BigDecimal("420.00"))),
                new BigDecimal("420.00"), "TRANSFERENCIA",
                new ComprobanteDTO("https://firmado/x", "x.jpg", null, "2026-11-20T09:00:00-07:00", "FLY-000128", "COMPLETADO"),
                new DatosEnvioDTO("Estafeta", "7210 4455", false),
                List.of(new HistorialEntradaDTO("2026-11-20T11:00:00-07:00", "Freddy Castro", "PAGADO", "ENVIADO", null)));
    }

    @Test void laListaRecibeLosFiltrosYRespondeEnSnakeCase() throws Exception {
        mvc.perform(get("/api/admin/pedidos").param("texto", "ana").param("metodo_pago", "EFECTIVO").param("desde", "2026-11-19").param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_encontrado").value(1))
                .andExpect(jsonPath("$.pedidos[0].folio").value("FLY-000128"))
                .andExpect(jsonPath("$.pedidos[0].fecha_creacion").value("2026-11-20T08:00:00-07:00"))
                .andExpect(jsonPath("$.pedidos[0].cliente_nombre").value("Ana Torres"))
                .andExpect(jsonPath("$.pedidos[0].cliente_telefono").value("644 123 4567"))
                .andExpect(jsonPath("$.pedidos[0].metodo_pago").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$.pedidos[0].total").value(420.00))
                .andExpect(jsonPath("$.pedidos[0].fecha_limite").value((Object) null));
        assertEquals(new BuscarPedidosPeticionDTO("ana", null, "EFECTIVO", "2026-11-19", null, 2), llamadas.get(0)[1]);

        mvc.perform(get("/api/admin/pedidos")).andExpect(status().isOk());
        assertEquals(new BuscarPedidosPeticionDTO(null, null, null, null, null, 1), llamadas.get(1)[1], "Sin filtros pide la primera página");
        mvc.perform(get("/api/admin/pedidos").param("pagina", "dos")).andExpect(status().isBadRequest());
    }

    @Test void elDetalleRespondeTodosLosCamposDelContrato() throws Exception {
        mvc.perform(get("/api/admin/pedidos/FLY-000128"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos_entrega").value("Calle Uno 123, Ciudad Obregón, Sonora"))
                .andExpect(jsonPath("$.cliente.id").value(7))
                .andExpect(jsonPath("$.lineas[0].precio_unitario").value(210.00))
                .andExpect(jsonPath("$.comprobante.nombre_archivo").value("x.jpg"))
                .andExpect(jsonPath("$.comprobante.monto").value((Object) null))
                .andExpect(jsonPath("$.envio.entrega_local").value(false))
                .andExpect(jsonPath("$.historial[0].fecha_hora").value("2026-11-20T11:00:00-07:00"))
                .andExpect(jsonPath("$.historial[0].estado_anterior").value("PAGADO"))
                .andExpect(jsonPath("$.historial[0].estado_nuevo").value("ENVIADO"));
        assertEquals("FLY-000128", llamadas.get(0)[1]);
    }

    @Test void lasAccionesLeenElCuerpoEnSnakeCaseYTomanAlUsuarioDeLaSesion() throws Exception {
        mvc.perform(post("/api/admin/pedidos/FLY-000127/pago-efectivo").principal(SESION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto_recibido\": 500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacion_enviada").value(false))
                .andExpect(jsonPath("$.pedido.folio").value("FLY-000128"));
        assertArrayEquals(new Object[]{"registrarPagoEfectivo", "FLY-000127", new BigDecimal("500"), UUID.fromString(ADMIN)}, llamadas.get(0));

        mvc.perform(post("/api/admin/pedidos/FLY-000128/aprobar-pago").principal(SESION)).andExpect(status().isOk());
        assertArrayEquals(new Object[]{"aprobarPago", "FLY-000128", UUID.fromString(ADMIN)}, llamadas.get(1));

        mvc.perform(post("/api/admin/pedidos/FLY-000128/rechazar-comprobante").principal(SESION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"El monto no coincide\", \"mensaje\": null}"))
                .andExpect(status().isOk());
        assertArrayEquals(new Object[]{"rechazarComprobante", "FLY-000128", "El monto no coincide", null, UUID.fromString(ADMIN)}, llamadas.get(2));

        mvc.perform(patch("/api/admin/pedidos/FLY-000128/estado").principal(SESION).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"ENVIADO\", \"paqueteria\": \"Estafeta\", \"guia\": \"7210 4455\", \"entrega_local\": false}"))
                .andExpect(status().isOk());
        assertArrayEquals(new Object[]{"actualizarEstado", "FLY-000128", "ENVIADO", new DatosEnvio("Estafeta", "7210 4455", false), UUID.fromString(ADMIN)}, llamadas.get(3));

        mvc.perform(patch("/api/admin/pedidos/FLY-000128/estado").principal(SESION).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\": \"ENVIADO\", \"paqueteria\": null, \"guia\": null, \"entrega_local\": true}"))
                .andExpect(status().isOk());
        assertEquals(new DatosEnvio(null, null, true), llamadas.get(4)[3]);

        mvc.perform(post("/api/admin/pedidos/FLY-000128/cancelar").principal(SESION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"La clienta pidió cancelar\"}"))
                .andExpect(status().isOk());
        assertArrayEquals(new Object[]{"cancelarPedido", "FLY-000128", "La clienta pidió cancelar", UUID.fromString(ADMIN)}, llamadas.get(5));
    }

    @Test void losErroresDeNegocioSeTraducenA404409Y400() throws Exception {
        error = new NegocioException(TipoError.NO_ENCONTRADO, "No existe un pedido con ese folio.");
        mvc.perform(get("/api/admin/pedidos/FLY-000999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("No existe un pedido con ese folio."));
        error = new NegocioException(TipoError.CONFLICTO_DE_DATOS, "El pedido ya está cancelado.");
        mvc.perform(post("/api/admin/pedidos/FLY-000128/cancelar").principal(SESION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"x\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("El pedido ya está cancelado."));
        error = new NegocioException(TipoError.DATOS_INVALIDOS, "Indica paquetería y número de guía, o marca entrega local.");
        mvc.perform(patch("/api/admin/pedidos/FLY-000128/estado").principal(SESION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\": \"ENVIADO\"}"))
                .andExpect(status().isBadRequest());
        error = null;
        mvc.perform(post("/api/admin/pedidos/FLY-000128/cancelar").principal(SESION)).andExpect(status().isBadRequest());
    }
}
