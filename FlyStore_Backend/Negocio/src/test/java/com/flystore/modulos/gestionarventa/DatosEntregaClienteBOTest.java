package com.flystore.modulos.gestionarventa;

import com.flystore.contratos.gestionarventa.DatosEntregaDTO;
import com.flystore.modulos.autenticacion.aplicacion.ClienteBO;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.entidades.DatosEntrega;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

class DatosEntregaClienteBOTest {
    private final com.flystore.persistencia.entidades.Cliente cliente = DatosVenta.cliente(7);
    private final List<DatosEntrega> guardados = new ArrayList<>();
    private final ClienteBO bo = new ClienteBO(puerto(IClienteDAO.class, (m, a) -> switch (m) {
        case "obtenerPorId" -> Optional.of(cliente);
        case "guardarDatosEntrega" -> {
            var datos = DatosVenta.conId((DatosEntrega) a[0], 50 + guardados.size());
            guardados.add(datos);
            cliente.getDatosEntrega().add(datos);
            yield null;
        }
        default -> throw new AssertionError(m);
    }), new PedidoVentaMapper());

    private static DatosEntregaDTO datos(String direccion, boolean predeterminada) {
        return new DatosEntregaDTO("Ana López", "6621234567", "Hermosillo", "Sonora", direccion, " ", predeterminada);
    }

    @Test void clienteNuevoRecibeSuNombreParaCompletar() {
        cliente.setTelefono("6620000000");
        var dto = bo.obtenerDatosEntrega(7L);
        assertEquals("Ana López", dto.nombreCompleto());
        assertEquals("6620000000", dto.telefono());
        assertNull(dto.direccion());
        assertFalse(dto.guardarComoPredeterminada());
    }

    @Test void losMismosDatosNoSeGuardanDosVecesYQuedanPredeterminados() {
        var primero = bo.registrarDatosEntrega(7L, datos("Calle Uno 123", true));
        var segundo = bo.registrarDatosEntrega(7L, datos("Calle Uno 123", false));
        assertSame(primero, segundo);
        assertEquals(1, guardados.size());
        assertNull(primero.getReferencias(), "Referencias en blanco se guardan como null");
        assertSame(primero, cliente.getDireccionPredeterminada());
        assertTrue(bo.obtenerDatosEntrega(7L).guardarComoPredeterminada());

        bo.registrarDatosEntrega(7L, datos("Calle Dos 456", false));
        assertEquals(2, guardados.size());
        assertEquals("Calle Uno 123", bo.obtenerDatosEntrega(7L).direccion(), "Sin marcarla, la nueva no sustituye a la predeterminada");
    }
}
