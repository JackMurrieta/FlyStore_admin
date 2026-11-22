package com.flystore.modulos.perfil;

import com.flystore.contratos.perfil.ActualizarPerfilPeticionDTO;
import com.flystore.contratos.perfil.DireccionEntregaDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.puertos.IProveedorAutenticacion;
import com.flystore.modulos.perfil.aplicacion.PerfilBO;
import com.flystore.modulos.perfil.aplicacion.mappers.PerfilMapper;
import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;
import com.flystore.persistencia.entidades.TipoCliente;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

class PerfilBOTest {
    private final UUID authId = UUID.randomUUID();
    private final Cliente cliente = new Cliente();
    private final AtomicInteger guardados = new AtomicInteger();

    private PerfilBO servicio(IdentidadAutenticada identidad) {
        cliente.setNombre("Ana");
        cliente.setApellidos("Torres");
        cliente.setEmail("ana@correo.mx");
        cliente.setTipoCliente(TipoCliente.BASE);
        return new PerfilBO(
                puerto(IClienteDAO.class, (m, a) -> {
                    if (m.equals("save")) { guardados.incrementAndGet(); return a[0]; }
                    return Optional.of(cliente);
                }),
                puerto(IEmpleadoDAO.class, (m, a) -> Optional.empty()),
                puerto(IAdminDAO.class, (m, a) -> Optional.empty()),
                puerto(IProveedorAutenticacion.class, (m, a) -> identidad),
                new PerfilMapper());
    }

    private IdentidadAutenticada identidad(String correo, String pendiente, List<String> proveedores) {
        return new IdentidadAutenticada(authId.toString(), correo, Map.of(), pendiente, proveedores);
    }

    private static DatosEntrega direccion(long id, String calle, Boolean activo) {
        var d = new DatosEntrega();
        try {
            var campo = DatosEntrega.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(d, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        d.setDireccion(calle);
        d.setActivo(activo);
        return d;
    }

    @Test
    void perfilSincronizaElCorreoConfirmadoYReportaComoEntra() {
        var perfil = servicio(identidad("nuevo@correo.mx", null, List.of("google"))).obtenerPerfil(authId, "token");
        assertEquals("nuevo@correo.mx", perfil.email());
        assertEquals("nuevo@correo.mx", cliente.getEmail());
        assertEquals(1, guardados.get());
        assertEquals("cliente", perfil.tipoUsuario());
        assertEquals("BASE", perfil.tipoCliente());
        assertFalse(perfil.tieneContrasena());
    }

    @Test
    void perfilMuestraElCorreoPendienteSinCambiarElActual() {
        var perfil = servicio(identidad("ana@correo.mx", "ana.nueva@correo.mx", List.of("email"))).obtenerPerfil(authId, "token");
        assertEquals("ana@correo.mx", perfil.email());
        assertEquals("ana.nueva@correo.mx", perfil.correoPendiente());
        assertTrue(perfil.tieneContrasena());
        assertEquals(0, guardados.get());
    }

    @Test
    void sesionDeOtroUsuarioSeRechaza() {
        var ajena = new IdentidadAutenticada(UUID.randomUUID().toString(), "x@correo.mx", Map.of());
        var ex = assertThrows(NegocioException.class, () -> servicio(ajena).obtenerPerfil(authId, "token"));
        assertEquals(TipoError.ACCESO_DENEGADO, ex.getTipoError());
    }

    @Test
    void actualizarPerfilRecortaYDejaSoloDigitosDelTelefono() {
        var perfil = servicio(identidad("ana@correo.mx", null, List.of("email")))
                .actualizarPerfil(authId, "token", new ActualizarPerfilPeticionDTO("  Ana María ", " Torres Ruiz ", "6441234567"));
        assertEquals("Ana María", perfil.nombre());
        assertEquals("Torres Ruiz", perfil.apellidos());
        assertEquals("6441234567", cliente.getTelefono());

        servicio(identidad("ana@correo.mx", null, List.of("email")))
                .actualizarPerfil(authId, "token", new ActualizarPerfilPeticionDTO("Ana", "Torres", ""));
        assertNull(cliente.getTelefono());
    }

    @Test
    void direccionesActivasConLaPredeterminadaPrimero() {
        var casa = direccion(1, "Casa", true);
        var oficina = direccion(2, "Oficina", true);
        var vieja = direccion(3, "Vieja", false);
        cliente.setDatosEntrega(new ArrayList<>(List.of(casa, oficina, vieja)));
        cliente.setDireccionPredeterminada(casa);
        List<DireccionEntregaDTO> lista = servicio(identidad("ana@correo.mx", null, List.of())).listarDirecciones(authId);
        assertEquals(List.of(1L, 2L), lista.stream().map(DireccionEntregaDTO::id).toList());
        assertTrue(lista.get(0).predeterminada());
        assertFalse(lista.get(1).predeterminada());
    }

    @Test
    void marcarQuitarYEliminarPredeterminada() {
        var casa = direccion(1, "Casa", true);
        var oficina = direccion(2, "Oficina", null);
        cliente.setDatosEntrega(new ArrayList<>(List.of(casa, oficina)));
        var bo = servicio(identidad("ana@correo.mx", null, List.of()));

        var lista = bo.marcarPredeterminada(authId, 2L);
        assertSame(oficina, cliente.getDireccionPredeterminada());
        assertEquals(2L, lista.get(0).id());

        bo.quitarPredeterminada(authId);
        assertNull(cliente.getDireccionPredeterminada());

        bo.marcarPredeterminada(authId, 1L);
        lista = bo.eliminarDireccion(authId, 1L);
        assertFalse(casa.getActivo());
        assertNull(cliente.getDireccionPredeterminada());
        assertEquals(List.of(2L), lista.stream().map(DireccionEntregaDTO::id).toList());
    }

    @Test
    void direccionInexistenteOInactivaNoSeEncuentra() {
        cliente.setDatosEntrega(new ArrayList<>(List.of(direccion(1, "Casa", false))));
        var bo = servicio(identidad("ana@correo.mx", null, List.of()));
        assertEquals(TipoError.NO_ENCONTRADO, assertThrows(NegocioException.class, () -> bo.marcarPredeterminada(authId, 1L)).getTipoError());
        assertEquals(TipoError.NO_ENCONTRADO, assertThrows(NegocioException.class, () -> bo.eliminarDireccion(authId, 99L)).getTipoError());
    }
}
