package com.flystore.modulos.gestionarventa;

import com.flystore.compartido.presentacion.GlobalExceptionHandler;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.IClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.IDatoBancarioBO;
import com.flystore.modulos.gestionarventa.aplicacion.IPedidoVentaBO;
import com.flystore.modulos.gestionarventa.presentacion.GestionarVentaController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CU-02 · El cliente cancela su pedido en POST /api/ventas/pedidos/{folio}/cancelar. El id del
 * cliente sale de la sesión; el motivo es opcional y el cuerpo también.
 */
class CancelarPedidoClienteControllerTest {
    private static final String CLIENTE = "22222222-2222-2222-2222-222222222222";
    private static final TestingAuthenticationToken SESION = new TestingAuthenticationToken(CLIENTE, null);

    private final List<Object[]> llamadas = new ArrayList<>();
    private NegocioException error;

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new GestionarVentaController(
                    puerto(IPedidoVentaBO.class, (metodo, argumentos) -> {
                        var llamada = new ArrayList<Object>(List.of(metodo));
                        llamada.addAll(Arrays.asList(argumentos));
                        llamadas.add(llamada.toArray());
                        if (error != null) throw error;
                        return null;
                    }),
                    puerto(IClienteBO.class, (metodo, argumentos) -> {
                        assertEquals("obtenerIdCliente", metodo);
                        assertEquals(UUID.fromString(CLIENTE), argumentos[0]);
                        return 7L;
                    }),
                    puerto(IDatoBancarioBO.class, (metodo, argumentos) -> null)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test void cancelaConElClienteDeLaSesionYMotivoOpcional() throws Exception {
        mvc.perform(post("/api/ventas/pedidos/FLY-000127/cancelar").principal(SESION)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"Ya no lo necesito\"}"))
                .andExpect(status().isOk());
        assertArrayEquals(new Object[]{"cancelarPedidoCliente", 7L, "FLY-000127", "Ya no lo necesito"}, llamadas.get(0));

        mvc.perform(post("/api/ventas/pedidos/FLY-000127/cancelar").principal(SESION)).andExpect(status().isOk());
        assertArrayEquals(new Object[]{"cancelarPedidoCliente", 7L, "FLY-000127", null}, llamadas.get(1), "Sin cuerpo no hay motivo");
    }

    @Test void unPedidoQueYaNoSeCancelaResponde409() throws Exception {
        error = new NegocioException(TipoError.CONFLICTO_DE_DATOS,
                "Este pedido ya no se puede cancelar: solo se cancela mientras está apartado o esperando tu comprobante.");
        mvc.perform(post("/api/ventas/pedidos/FLY-000128/cancelar").principal(SESION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO_DE_DATOS"));
    }
}
