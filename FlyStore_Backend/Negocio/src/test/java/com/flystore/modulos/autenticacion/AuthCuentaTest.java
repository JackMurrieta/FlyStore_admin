package com.flystore.modulos.autenticacion;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.AuthBO;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.modelo.SesionAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.puertos.IProveedorAutenticacion;
import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.TipoCliente;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import static com.flystore.soporte.Dobles.puerto;
import static org.junit.jupiter.api.Assertions.*;

/** Seguridad de la cuenta: correo, contraseña, enlaces de correo y Google. */
class AuthCuentaTest {
    private final UUID id = UUID.randomUUID();
    private final AtomicReference<Cliente> existente = new AtomicReference<>();
    private final AtomicReference<Cliente> guardado = new AtomicReference<>();
    private final List<String> llamadas = new ArrayList<>();

    private AuthBO servicio(BiFunction<String, Object[], Object> proveedor) {
        return new AuthBO(
                puerto(IProveedorAutenticacion.class, (m, a) -> { llamadas.add(m); return proveedor.apply(m, a); }),
                puerto(IClienteDAO.class, (m, a) -> {
                    if (m.equals("save")) { guardado.set((Cliente) a[0]); existente.set((Cliente) a[0]); return a[0]; }
                    return Optional.ofNullable(existente.get());
                }),
                puerto(IEmpleadoDAO.class, (m, a) -> Optional.empty()),
                puerto(IAdminDAO.class, (m, a) -> Optional.empty()),
                () -> List.of(TipoCliente.values()));
    }

    private IdentidadAutenticada identidad(String correo, List<String> proveedores, Map<String, Object> metadata) {
        return new IdentidadAutenticada(id.toString(), correo, metadata, null, proveedores);
    }

    private SesionAutenticada sesion(IdentidadAutenticada usuario) {
        return new SesionAutenticada("nuevo-token", "bearer", 3600, "nuevo-refresh", usuario);
    }

    private Cliente clienteExistente() {
        var c = new Cliente();
        c.setNombre("Ana");
        c.setApellidos("Torres");
        c.setEmail("ana@correo.mx");
        existente.set(c);
        return c;
    }

    @Test
    void cambiarContrasenaVerificaLaActualYUsaLaSesionVerificada() {
        clienteExistente();
        var ident = identidad("ana@correo.mx", List.of("email"), Map.of());
        var tokenUsado = new AtomicReference<String>();
        var respuesta = servicio((m, a) -> switch (m) {
            case "obtenerUsuario" -> ident;
            case "login" -> sesion(ident);
            case "actualizarContrasena" -> { tokenUsado.set((String) a[0]); yield null; }
            default -> null;
        }).cambiarContrasena("token-viejo", "Secreta123", "NuevaSecreta456");
        assertEquals("nuevo-token", tokenUsado.get());
        assertEquals("nuevo-refresh", respuesta.refreshToken());
    }

    @Test
    void contrasenaActualIncorrectaEsErrorDeDatosNoDeSesion() {
        var ident = identidad("ana@correo.mx", List.of("email"), Map.of());
        var ex = assertThrows(NegocioException.class, () -> servicio((m, a) -> {
            if (m.equals("login")) throw new NegocioException(TipoError.CREDENCIALES_INVALIDAS, "x");
            return ident;
        }).cambiarContrasena("t", "mala", "NuevaSecreta456"));
        assertEquals(TipoError.DATOS_INVALIDOS, ex.getTipoError());
        assertFalse(llamadas.contains("actualizarContrasena"));
    }

    @Test
    void cuentaSoloDeGoogleNoCambiaContrasenaNiCorreo() {
        var ident = identidad("ana@gmail.com", List.of("google"), Map.of());
        var bo = servicio((m, a) -> ident);
        assertEquals(TipoError.DATOS_INVALIDOS, assertThrows(NegocioException.class,
                () -> bo.cambiarContrasena("t", "a", "NuevaSecreta456")).getTipoError());
        assertEquals(TipoError.DATOS_INVALIDOS, assertThrows(NegocioException.class,
                () -> bo.solicitarCambioCorreo("t", "otro@correo.mx")).getTipoError());
        assertFalse(llamadas.contains("solicitarCambioCorreo"));
    }

    @Test
    void cambioDeCorreoRechazaElMismoCorreoYNormaliza() {
        var ident = identidad("ana@correo.mx", List.of("email"), Map.of());
        var enviado = new AtomicReference<String>();
        var bo = servicio((m, a) -> { if (m.equals("solicitarCambioCorreo")) enviado.set((String) a[1]); return ident; });
        assertThrows(NegocioException.class, () -> bo.solicitarCambioCorreo("t", " ANA@correo.mx "));
        bo.solicitarCambioCorreo("t", " Ana.Nueva@Correo.mx ");
        assertEquals("ana.nueva@correo.mx", enviado.get());
    }

    @Test
    void confirmarRegistroCreaAlClienteConSusDatos() {
        var ident = identidad("ana@correo.mx", List.of("email"),
                Map.of("nombre", "Ana", "apellidos", "Torres", "tipo_cliente", "MAYOREO", "telefono", "6441234567"));
        var confirmacion = servicio((m, a) -> sesion(ident)).confirmarCuenta("hash", "email");
        assertNotNull(confirmacion.sesion());
        assertEquals("Ana", guardado.get().getNombre());
        assertEquals(TipoCliente.MAYOREO, guardado.get().getTipoCliente());
        assertEquals("cliente", confirmacion.sesion().tipoUsuario());
    }

    @Test
    void confirmarCambioDeCorreoActualizaElUsuarioLocal() {
        var cliente = clienteExistente();
        var ident = identidad("ana.nueva@correo.mx", List.of("email"), Map.of());
        var confirmacion = servicio((m, a) -> sesion(ident)).confirmarCuenta("hash", "email_change");
        assertEquals("ana.nueva@correo.mx", cliente.getEmail());
        assertTrue(confirmacion.mensaje().contains("ana.nueva@correo.mx"));
    }

    @Test
    void cambioDeCorreoSeguroAvisaQueFaltaLaOtraConfirmacion() {
        var confirmacion = servicio((m, a) -> new SesionAutenticada(null, null, null, null, null))
                .confirmarCuenta("hash", "email_change");
        assertNull(confirmacion.sesion());
        assertTrue(confirmacion.mensaje().contains("otro correo"));
    }

    @Test
    void recuperacionNoCreaClientes() {
        var ident = identidad("nadie@correo.mx", List.of("email"), Map.of());
        var ex = assertThrows(NegocioException.class, () -> servicio((m, a) -> sesion(ident)).confirmarCuenta("hash", "recovery"));
        assertEquals(TipoError.USUARIO_NO_ENCONTRADO, ex.getTipoError());
        assertNull(guardado.get());
    }

    @Test
    void restablecerSoloConSesionDeRecuperacionReciente() {
        var bo = servicio((m, a) -> null);
        assertEquals(TipoError.ACCESO_DENEGADO,
                assertThrows(NegocioException.class, () -> bo.restablecerContrasena("t", false, "NuevaSecreta456")).getTipoError());
        assertFalse(llamadas.contains("actualizarContrasena"));
        bo.restablecerContrasena("t", true, "NuevaSecreta456");
        assertTrue(llamadas.contains("actualizarContrasena"));
    }

    @Test
    void googleCreaAlClienteNuevoConSuNombreCompleto() {
        var ident = identidad("ana@gmail.com", List.of("google"), Map.of("full_name", "Ana María Torres Ruiz"));
        var respuesta = servicio((m, a) -> sesion(ident)).completarSesionConProveedor("code", "verifier");
        assertEquals("Ana", guardado.get().getNombre());
        assertEquals("María Torres Ruiz", guardado.get().getApellidos());
        assertEquals(TipoCliente.BASE, guardado.get().getTipoCliente());
        assertEquals("nuevo-refresh", respuesta.refreshToken());
    }

    @Test
    void googleConClienteExistenteNoLoDuplica() {
        clienteExistente();
        var ident = identidad("ana@gmail.com", List.of("google"), Map.of("full_name", "Otra Persona"));
        servicio((m, a) -> sesion(ident)).completarSesionConProveedor("code", "verifier");
        assertNull(guardado.get());
    }

    @Test
    void soloGoogleComoProveedorExterno() {
        var bo = servicio((m, a) -> "https://supabase/authorize");
        assertEquals("https://supabase/authorize", bo.iniciarSesionConProveedor("google", "https://api/cb", "reto"));
        assertThrows(NegocioException.class, () -> bo.iniciarSesionConProveedor("github", "https://api/cb", "reto"));
    }
}
