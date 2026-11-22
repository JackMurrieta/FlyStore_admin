package com.flystore.modulos.autenticacion;

import com.flystore.contratos.autenticacion.*;
import com.flystore.dominio.excepcion.*;
import com.flystore.persistencia.entidades.*;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.modulos.autenticacion.aplicacion.AuthBO;
import com.flystore.modulos.autenticacion.aplicacion.modelo.*;
import com.flystore.modulos.autenticacion.aplicacion.puertos.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

class AuthBOTest {
    private final UUID id = UUID.randomUUID();
    private final Cliente cliente = new Cliente();

    private AuthBO servicio(IProveedorAutenticacion proveedor) {
        cliente.setActivo(true);
        return new AuthBO(proveedor,
                puerto(IClienteDAO.class, (m,a) -> m.equals("save") ? a[0] : Optional.of(cliente)),
                puerto(IEmpleadoDAO.class, (m,a) -> Optional.empty()),
                puerto(IAdminDAO.class, (m,a) -> Optional.empty()),
                () -> List.of(TipoCliente.values()));
    }

    private SesionAutenticada sesion(String refresh) {
        return new SesionAutenticada("access", "Bearer", 3600, refresh,
                new IdentidadAutenticada(id.toString(), "cliente@example.com", Map.of()));
    }

    @Test
    void loginYRefreshConservanTokenRotado() {
        var servicio = servicio(puerto(IProveedorAutenticacion.class,
                (m,a) -> sesion(m.equals("login") ? "inicial" : "rotado")));
        assertEquals("inicial", servicio.login(new LoginRequest("cliente@example.com", "secreto")).refreshToken());
        assertEquals("rotado", servicio.refrescarToken("inicial").refreshToken());
    }

    @Test
    void tipoInvalidoSeRechazaAntesDeCrearIdentidadExterna() {
        var llamadas = new AtomicInteger();
        var servicio = servicio(puerto(IProveedorAutenticacion.class, (m,a) -> {
            llamadas.incrementAndGet(); return sesion("refresh");
        }));
        var ex = assertThrows(NegocioException.class, () -> servicio.registrar(
                new RegisterRequest("Ana", "López", "ana@example.com", "secreto", null, "INVALIDO")));
        assertEquals(TipoError.DATOS_INVALIDOS, ex.getTipoError());
        assertEquals(0, llamadas.get());
    }

    @Test
    void registroPendienteNoPersisteClienteSinVerificacion() {
        var guardados = new AtomicInteger();
        var proveedor = puerto(IProveedorAutenticacion.class,
                (m,a) -> new SesionAutenticada(null, null, null, null, null));
        var servicio = new AuthBO(proveedor,
                puerto(IClienteDAO.class, (m,a) -> { guardados.incrementAndGet(); return a[0]; }),
                puerto(IEmpleadoDAO.class, (m,a) -> Optional.empty()),
                puerto(IAdminDAO.class, (m,a) -> Optional.empty()), () -> List.of(TipoCliente.values()));
        assertNull(servicio.registrar(new RegisterRequest("Ana", "López", "ana@example.com", "secreto", null, "BASE")));
        assertEquals(0, guardados.get());
    }

    @Test
    void callbackExistenteNoDuplicaClienteYConservaRefresh() {
        var servicio = servicio(puerto(IProveedorAutenticacion.class,
                (m,a) -> new IdentidadAutenticada(id.toString(), "cliente@example.com", Map.of())));
        assertEquals("refresh", servicio.procesarCallbackVerificacion("access", "refresh").refreshToken());
    }

    @Test
    void registroInmediatoMapeaClienteSinGuardarLaContrasena() {
        var guardado = new java.util.concurrent.atomic.AtomicReference<Cliente>();
        var servicio = new AuthBO(puerto(IProveedorAutenticacion.class, (m,a) -> sesion("refresh")),
                puerto(IClienteDAO.class, (m,a) -> { guardado.set((Cliente) a[0]); return a[0]; }),
                puerto(IEmpleadoDAO.class, (m,a) -> Optional.empty()),
                puerto(IAdminDAO.class, (m,a) -> Optional.empty()), () -> List.of(TipoCliente.values()));
        var resultado = servicio.registrar(new RegisterRequest("Ana", "López", "cliente@example.com",
                "secreto", "1234567890", "BASE"));
        assertEquals(id, guardado.get().getAuthId());
        assertEquals("Ana", guardado.get().getNombre());
        assertEquals(TipoCliente.BASE, guardado.get().getTipoCliente());
        assertNull(guardado.get().getPassword());
        assertTrue(guardado.get().getActivo());
        assertNotNull(guardado.get().getFechaRegistro());
        assertEquals("cliente", resultado.tipoUsuario());
        assertEquals("refresh", resultado.refreshToken());
    }
}
