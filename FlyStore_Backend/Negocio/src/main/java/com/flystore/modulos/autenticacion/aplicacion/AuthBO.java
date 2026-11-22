package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.modulos.autenticacion.aplicacion.modelo.AuthResponse;
import com.flystore.contratos.autenticacion.LoginRequest;
import com.flystore.contratos.autenticacion.RegisterRequest;
import com.flystore.modulos.autenticacion.aplicacion.modelo.SesionAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.modelo.ConfirmacionCuenta;
import java.util.Locale;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.TipoCliente;
import com.flystore.persistencia.entidades.Usuario;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.dao.interfaces.ITipoClienteDAO;
import com.flystore.modulos.autenticacion.aplicacion.puertos.IProveedorAutenticacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import com.flystore.persistencia.entidades.Admin;
import com.flystore.persistencia.entidades.Empleado;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business Object para autenticación
 * SOLID: SRP - Solo lógica de negocio de autenticación
 * GRASP: Information Expert - Coordina el flujo de autenticación
 * GRASP: Creator - Crea las entidades Usuario/Cliente/Empleado/Admin
 */
@Service
public class AuthBO implements IAuthBO {

    private static final Logger logger = LoggerFactory.getLogger(AuthBO.class);

    private final IProveedorAutenticacion supabaseGateway;
    private final IClienteDAO clienteDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IAdminDAO adminDAO;
    private final ITipoClienteDAO tipoClienteDAO;

    public AuthBO(IProveedorAutenticacion supabaseGateway,
                  IClienteDAO clienteDAO,
                  IEmpleadoDAO empleadoDAO,
                  IAdminDAO adminDAO,
                  ITipoClienteDAO tipoClienteDAO) {
        this.supabaseGateway = supabaseGateway;
        this.clienteDAO = clienteDAO;
        this.empleadoDAO = empleadoDAO;
        this.adminDAO = adminDAO;
        this.tipoClienteDAO = tipoClienteDAO;
    }

    @Override
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        logger.info("Registrando cliente: {} - Tipo: {}", request.email(), request.tipoCliente());

        validarTipoCliente(request.tipoCliente());
        try {
            // 1. Crear metadata para Supabase (información del usuario)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("nombre", request.nombre());
            metadata.put("apellidos", request.apellidos());
            metadata.put("tipo_cliente", request.tipoCliente());
            metadata.put("telefono", request.telefono() != null ? request.telefono() : "");

            // 2. Registrar usuario en Supabase Auth
            SesionAutenticada sesion = supabaseGateway.registrar(
                    request.email(),
                    request.password(),
                    metadata
            );

            // 3. Verificar si requiere confirmación de email
            // Si no hay access_token, significa que se envió email de verificación
            if (sesion.accessToken() == null || sesion.accessToken().isBlank()) {
                logger.info("Registro exitoso - Email de verificación enviado a: {}", request.email());
                return null; // El controller manejará este caso
            }

            // 4. Si hay access_token, crear cliente inmediatamente (email verification deshabilitada)
            UUID authId = UUID.fromString(sesion.user().id());
            Cliente cliente = crearClienteEntidad(authId, request);

            // 5. Construir respuesta
            return respuestaSesion(sesion, cliente);

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al registrar cliente", e);
            throw new NegocioException(TipoError.ERROR_INTERNO, "Error al registrar cliente");
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        logger.info("Iniciando sesión: {}", request.email());

        try {
            // 1. Login en Supabase Auth
            SesionAutenticada sesion = supabaseGateway.login(
                    request.email(),
                    request.password()
            );

            UUID authId = UUID.fromString(sesion.user().id());

            // 2. Buscar usuario en BD local
            Usuario usuario = buscarUsuarioPorAuthId(authId);

            if (usuario == null) {
                logger.warn("Usuario {} no encontrado en BD local", authId);
                throw new NegocioException(
                        TipoError.USUARIO_NO_ENCONTRADO,
                        "Usuario no registrado en el sistema"
                );
            }

            // 3. Construir respuesta
            return respuestaSesion(sesion, usuario);

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al hacer login", e);
            throw new NegocioException(TipoError.ERROR_INTERNO, "Error al iniciar sesión");
        }
    }

    @Override
    public AuthResponse refrescarToken(String refreshToken) {
        logger.debug("Refrescando token");

        SesionAutenticada sesion = supabaseGateway.refrescar(refreshToken);
        UUID authId = UUID.fromString(sesion.user().id());
        Usuario usuario = buscarUsuarioPorAuthId(authId);

        if (usuario == null) {
            throw new NegocioException(TipoError.USUARIO_NO_ENCONTRADO, "Usuario no encontrado");
        }

        return respuestaSesion(sesion, usuario);
    }

    @Override
    public void cerrarSesion(String accessToken) {
        logger.debug("Cerrando sesión");
        supabaseGateway.cerrarSesion(accessToken);
    }

    /**
     * Crea el cliente en la base de datos local
     * GRASP: Creator - AuthBO crea la entidad Cliente
     *
     * Nota: El registro público (/api/auth/registro) SIEMPRE crea Clientes.
     * Para crear Empleados/Admins se usarán endpoints protegidos específicos.
     *
     * @param authId UUID del usuario en Supabase Auth (FK)
     * @param request Datos del registro
     * @return Cliente creado
     */
    private TipoCliente validarTipoCliente(String valor) {
        try {
            return TipoCliente.valueOf(valor.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Tipo de cliente no válido");
        }
    }

    private Cliente crearClienteEntidad(UUID authId, RegisterRequest request) {
        // Validar que el tipo_cliente sea válido
        TipoCliente tipoCliente = validarTipoCliente(request.tipoCliente());

        Cliente cliente = new Cliente();
        cliente.setAuthId(authId); cliente.setNombre(request.nombre()); cliente.setApellidos(request.apellidos());
        cliente.setEmail(request.email()); cliente.setTelefono(request.telefono()); cliente.setTipoCliente(tipoCliente);
        cliente.setActivo(true); cliente.setFechaRegistro(LocalDate.now());

        Cliente clienteGuardado = clienteDAO.save(cliente);
        logger.info("Cliente creado en BD local - ID: {}, Auth ID: {}, Tipo: {}",
                clienteGuardado.getId(), authId, tipoCliente);

        return clienteGuardado;
    }

    /**
     * Busca un usuario por authId en todas las tablas
     * GRASP: Information Expert - Conoce cómo buscar usuarios
     */
    private Usuario buscarUsuarioPorAuthId(UUID authId) {
        return clienteDAO.findByAuthId(authId)
                .map(u -> (Usuario) u)
                .or(() -> empleadoDAO.findByAuthId(authId).map(u -> (Usuario) u))
                .or(() -> adminDAO.findByAuthId(authId).map(u -> (Usuario) u))
                .orElse(null);
    }



    @Override
    public List<String> obtenerTiposCliente() {
        logger.debug("Obteniendo tipos de cliente disponibles");

        // Obtener tipos de cliente desde DAO
        List<TipoCliente> tiposCliente = tipoClienteDAO.obtenerTodos();

        // Convertir enum a String
        return tiposCliente.stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AuthResponse procesarCallbackVerificacion(String accessToken, String refreshToken) {
        logger.info("Procesando callback de verificación de email");

        try {
            // 1. Obtener información del usuario desde Supabase
            IdentidadAutenticada usuario = supabaseGateway.obtenerUsuario(accessToken);
            UUID authId = UUID.fromString(usuario.id());

            // 2. Verificar si el cliente ya existe en BD local
            Optional<Cliente> clienteExistente = clienteDAO.findByAuthId(authId);

            Cliente cliente;
            if (clienteExistente.isPresent()) {
                // Cliente ya existe (puede ser una re-verificación)
                logger.info("Cliente ya existe en BD: {}", authId);
                cliente = clienteExistente.get();
            } else {
                // 3. Crear cliente desde metadata guardado en Supabase
                logger.info("Creando cliente en BD desde metadata de Supabase: {}", authId);

                Map<String, Object> metadata = usuario.userMetadata();
                String nombre = (String) metadata.get("nombre");
                String apellidos = (String) metadata.get("apellidos");
                String tipoClienteStr = (String) metadata.get("tipo_cliente");
                String telefono = (String) metadata.get("telefono");

                // Validar metadata
                if (nombre == null || apellidos == null || tipoClienteStr == null) {
                    throw new NegocioException(
                            TipoError.DATOS_INVALIDOS,
                            "Metadata de usuario incompleta en Supabase"
                    );
                }

                // Crear RegisterRequest desde metadata
                RegisterRequest request = new RegisterRequest(
                        nombre,
                        apellidos,
                        usuario.email(),
                        "", // password no es necesario aquí
                        telefono,
                        tipoClienteStr
                );

                cliente = crearClienteEntidad(authId, request);
            }

            // 4. Construir respuesta con tokens
            return respuesta(accessToken, 3600L, "Bearer", authId, usuario.email(), cliente, refreshToken);

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al procesar callback de verificación", e);
            throw new NegocioException(TipoError.ERROR_INTERNO, "Error al procesar verificación de email");
        }
    }

    @Override
    public AuthResponse obtenerUsuarioActual(String accessToken) {
        logger.info("Obteniendo información del usuario actual");

        try {
            // 1. Validar token con Supabase y obtener información del usuario
            IdentidadAutenticada usuarioSupabase = supabaseGateway.obtenerUsuario(accessToken);
            UUID authId = UUID.fromString(usuarioSupabase.id());

            // 2. Buscar usuario en BD local
            Usuario usuario = buscarUsuarioPorAuthId(authId);

            if (usuario == null) {
                logger.warn("Usuario {} no encontrado en BD local", authId);
                throw new NegocioException(
                        TipoError.USUARIO_NO_ENCONTRADO,
                        "Usuario no registrado en el sistema"
                );
            }

            // 3. Construir respuesta
            return respuesta(accessToken, 3600L, "Bearer", authId, usuarioSupabase.email(), usuario, null);

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al obtener usuario actual", e);
            throw new NegocioException(TipoError.TOKEN_INVALIDO, "Token de sesión inválido o expirado");
        }
    }

    // ===== Cuenta (perfil · seguridad) =====

    /** Tipos de enlace de correo que confirman el registro y pueden crear al cliente. */
    private static final java.util.Set<String> TIPOS_REGISTRO = java.util.Set.of("email", "signup", "invite", "magiclink");

    @Override
    public void solicitarCambioCorreo(String accessToken, String nuevoCorreo) {
        var correo = nuevoCorreo.trim().toLowerCase(Locale.ROOT);
        IdentidadAutenticada identidad = supabaseGateway.obtenerUsuario(accessToken);
        if (correo.equalsIgnoreCase(identidad.email())) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Ese ya es tu correo actual");
        }
        if (!identidad.tieneContrasena()) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS,
                    "Tu cuenta entra con Google; el correo es el de tu cuenta de Google y no se cambia aquí");
        }
        supabaseGateway.solicitarCambioCorreo(accessToken, correo);
        logger.info("Cambio de correo solicitado para {}", identidad.id());
    }

    @Override
    public AuthResponse cambiarContrasena(String accessToken, String actual, String nueva) {
        IdentidadAutenticada identidad = supabaseGateway.obtenerUsuario(accessToken);
        if (!identidad.tieneContrasena()) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS,
                    "Tu cuenta entra con Google, así que no tiene contraseña que cambiar");
        }
        if (actual.equals(nueva)) {
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "La nueva contraseña debe ser distinta de la actual");
        }
        SesionAutenticada verificada;
        try {
            verificada = supabaseGateway.login(identidad.email(), actual);
        } catch (NegocioException e) {
            // 400 y no 401: un 401 haría que el frontend crea que la sesión expiró.
            if (e.getTipoError() == TipoError.CREDENCIALES_INVALIDAS)
                throw new NegocioException(TipoError.DATOS_INVALIDOS, "La contraseña actual no es correcta");
            throw e;
        }
        if (verificada.user() == null || !identidad.id().equals(verificada.user().id())) {
            throw new NegocioException(TipoError.ACCESO_DENEGADO, "No pudimos verificar tu cuenta");
        }
        supabaseGateway.actualizarContrasena(verificada.accessToken(), nueva);
        Usuario usuario = buscarUsuarioPorAuthId(UUID.fromString(identidad.id()));
        if (usuario == null) throw new NegocioException(TipoError.USUARIO_NO_ENCONTRADO, "Usuario no registrado en el sistema");
        logger.info("Contraseña actualizada para {}", identidad.id());
        return respuestaSesion(verificada, usuario);
    }

    @Override
    public void recuperarContrasena(String correo) {
        // El proveedor responde igual exista o no el correo: no se revela quién tiene cuenta.
        supabaseGateway.enviarRecuperacion(correo.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    @Transactional
    public ConfirmacionCuenta confirmarCuenta(String tokenHash, String tipo) {
        SesionAutenticada sesion = supabaseGateway.verificarToken(tokenHash, tipo);
        if (sesion.accessToken() == null || sesion.user() == null) {
            return new ConfirmacionCuenta(tipo, null,
                    "Confirmamos este correo. Abre también el enlace que enviamos al otro correo para terminar el cambio.");
        }
        IdentidadAutenticada identidad = sesion.user();
        Usuario usuario = TIPOS_REGISTRO.contains(tipo)
                ? obtenerOCrearCliente(identidad)
                : buscarUsuarioPorAuthId(UUID.fromString(identidad.id()));
        if (usuario == null) throw new NegocioException(TipoError.USUARIO_NO_ENCONTRADO, "Usuario no registrado en el sistema");

        String mensaje;
        switch (tipo) {
            case "recovery" -> mensaje = "Escribe tu nueva contraseña.";
            case "email_change" -> {
                sincronizarCorreo(usuario, identidad.email());
                mensaje = "Listo, tu correo ahora es " + identidad.email() + ".";
            }
            default -> mensaje = "Tu correo quedó confirmado. ¡Bienvenido a FlyStore!";
        }
        return new ConfirmacionCuenta(tipo, respuestaSesion(sesion, usuario), mensaje);
    }

    @Override
    public void restablecerContrasena(String accessToken, boolean recuperacionReciente, String nueva) {
        if (!recuperacionReciente) {
            throw new NegocioException(TipoError.ACCESO_DENEGADO,
                    "El enlace para restablecer tu contraseña venció. Pide uno nuevo.");
        }
        supabaseGateway.actualizarContrasena(accessToken, nueva);
    }

    @Override
    public void cerrarSesionGlobal(String accessToken) {
        supabaseGateway.cerrarSesionGlobal(accessToken);
    }

    @Override
    public String iniciarSesionConProveedor(String proveedor, String redirigirA, String codeChallenge) {
        if (!"google".equals(proveedor)) throw new NegocioException(TipoError.DATOS_INVALIDOS, "Proveedor no disponible");
        return supabaseGateway.urlAutorizacion(proveedor, redirigirA, codeChallenge);
    }

    @Override
    @Transactional
    public AuthResponse completarSesionConProveedor(String codigo, String codeVerifier) {
        SesionAutenticada sesion = supabaseGateway.canjearCodigo(codigo, codeVerifier);
        if (sesion.user() == null) throw new NegocioException(TipoError.TOKEN_INVALIDO, "No pudimos completar el inicio de sesión");
        return respuestaSesion(sesion, obtenerOCrearCliente(sesion.user()));
    }

    /**
     * Usuario local de la identidad; si no existe, crea un Cliente BASE con lo que trae el proveedor
     * (registro por correo: nombre, apellidos, teléfono y tipo; Google: nombre completo).
     */
    private Usuario obtenerOCrearCliente(IdentidadAutenticada identidad) {
        UUID authId = UUID.fromString(identidad.id());
        Usuario existente = buscarUsuarioPorAuthId(authId);
        if (existente != null) return existente;

        Map<String, Object> metadata = identidad.userMetadata();
        String nombreCompleto = texto(metadata, "full_name", texto(metadata, "name", ""));
        int espacio = nombreCompleto.indexOf(' ');
        String nombre = texto(metadata, "nombre", texto(metadata, "given_name",
                espacio > 0 ? nombreCompleto.substring(0, espacio) : nombreCompleto));
        String apellidos = texto(metadata, "apellidos", texto(metadata, "family_name",
                espacio > 0 ? nombreCompleto.substring(espacio + 1) : ""));
        if (nombre.isBlank() && identidad.email() != null) nombre = identidad.email().split("@")[0];
        String telefono = texto(metadata, "telefono", "");

        logger.info("Creando cliente para identidad nueva {}", authId);
        return crearClienteEntidad(authId, new RegisterRequest(nombre, apellidos, identidad.email(), "",
                telefono.isBlank() ? null : telefono, texto(metadata, "tipo_cliente", "BASE")));
    }

    private static String texto(Map<String, Object> metadata, String clave, String porDefecto) {
        Object valor = metadata.get(clave);
        return valor instanceof String s && !s.isBlank() ? s.trim() : porDefecto;
    }

    /** El correo de Supabase manda: tras confirmar el cambio se copia al usuario local. */
    private void sincronizarCorreo(Usuario usuario, String correo) {
        if (correo == null || correo.equalsIgnoreCase(usuario.getEmail())) return;
        usuario.setEmail(correo);
        if (usuario instanceof Cliente c) clienteDAO.save(c);
        else if (usuario instanceof Empleado e) empleadoDAO.save(e);
        else if (usuario instanceof Admin a) adminDAO.save(a);
    }

    private AuthResponse respuestaSesion(SesionAutenticada sesion, Usuario usuario) {
        return respuesta(sesion.accessToken(), sesion.expiresIn().longValue(), sesion.tokenType(),
                UUID.fromString(sesion.user().id()), sesion.user().email(), usuario, sesion.refreshToken());
    }
    private AuthResponse respuesta(String token, Long expires, String tokenType, UUID id, String email, Usuario usuario, String refresh) {
        String tipo = usuario instanceof Cliente ? "cliente" : usuario instanceof Empleado ? "empleado" : usuario instanceof Admin ? "admin" : "desconocido";
        return new AuthResponse(token, expires, tokenType, id, email, usuario.getNombre(), usuario.getApellidos(), tipo, refresh);
    }
}
