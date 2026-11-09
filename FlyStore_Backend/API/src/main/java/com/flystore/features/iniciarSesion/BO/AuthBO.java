package com.flystore.features.iniciarSesion.BO;

import CU_iniciarSesion_DTOs.AuthResponse;
import CU_iniciarSesion_DTOs.LoginRequest;
import CU_iniciarSesion_DTOs.RegisterRequest;
import CU_iniciarSesion_DTOs.SupabaseSession;
import CU_iniciarSesion_DTOs.SupabaseUser;
import com.flystore.config.SupabaseProperties;
import com.flystore.entidades.Admin;
import com.flystore.entidades.Cliente;
import com.flystore.entidades.Empleado;
import com.flystore.entidades.TipoCliente;
import com.flystore.entidades.Usuario;
import com.flystore.excepcion.NegocioException;
import com.flystore.excepcion.TipoError;
import com.flystore.features.iniciarSesion.DAO.IAdminDAO;
import com.flystore.features.iniciarSesion.DAO.IClienteDAO;
import com.flystore.features.iniciarSesion.DAO.IEmpleadoDAO;
import com.flystore.features.iniciarSesion.DAO.ITipoClienteDAO;
import com.flystore.gateway.ISupabaseAuthGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final ISupabaseAuthGateway supabaseGateway;
    private final IClienteDAO clienteDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IAdminDAO adminDAO;
    private final SupabaseProperties supabaseProperties;
    private final ITipoClienteDAO tipoClienteDAO;

    public AuthBO(ISupabaseAuthGateway supabaseGateway,
                  IClienteDAO clienteDAO,
                  IEmpleadoDAO empleadoDAO,
                  IAdminDAO adminDAO,
                  SupabaseProperties supabaseProperties,
                  ITipoClienteDAO tipoClienteDAO) {
        this.supabaseGateway = supabaseGateway;
        this.clienteDAO = clienteDAO;
        this.empleadoDAO = empleadoDAO;
        this.adminDAO = adminDAO;
        this.supabaseProperties = supabaseProperties;
        this.tipoClienteDAO = tipoClienteDAO;
    }

    @Override
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        logger.info("Registrando cliente: {} - Tipo: {}", request.email(), request.tipoCliente());

        try {
            // 1. Crear metadata para Supabase (información del usuario)
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("nombre", request.nombre());
            metadata.put("apellidos", request.apellidos());
            metadata.put("tipo_cliente", request.tipoCliente());
            metadata.put("telefono", request.telefono() != null ? request.telefono() : "");

            // 2. Registrar usuario en Supabase Auth
            SupabaseSession sesion = supabaseGateway.registrar(
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
            return new AuthResponse(
                    sesion.accessToken(),
                    sesion.expiresIn().longValue(),
                    sesion.tokenType(),
                    authId,
                    sesion.user().email(),
                    cliente.getNombre(),
                    cliente.getApellidos(),
                    "cliente"
            );

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
            SupabaseSession sesion = supabaseGateway.login(
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
            return new AuthResponse(
                    sesion.accessToken(),
                    sesion.expiresIn().longValue(),
                    sesion.tokenType(),
                    authId,
                    sesion.user().email(),
                    usuario.getNombre(),
                    usuario.getApellidos(),
                    determinarTipoUsuario(usuario)
            );

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

        SupabaseSession sesion = supabaseGateway.refrescar(refreshToken);
        UUID authId = UUID.fromString(sesion.user().id());
        Usuario usuario = buscarUsuarioPorAuthId(authId);

        if (usuario == null) {
            throw new NegocioException(TipoError.USUARIO_NO_ENCONTRADO, "Usuario no encontrado");
        }

        return new AuthResponse(
                sesion.accessToken(),
                sesion.expiresIn().longValue(),
                sesion.tokenType(),
                authId,
                sesion.user().email(),
                usuario.getNombre(),
                usuario.getApellidos(),
                determinarTipoUsuario(usuario)
        );
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
    @Transactional
    private Cliente crearClienteEntidad(UUID authId, RegisterRequest request) {
        // Validar que el tipo_cliente sea válido
        TipoCliente tipoCliente;
        try {
            tipoCliente = TipoCliente.valueOf(request.tipoCliente().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Tipo de cliente no válido: {}", request.tipoCliente());
            throw new NegocioException(
                    TipoError.DATOS_INVALIDOS,
                    "Tipo de cliente no válido: " + request.tipoCliente()
            );
        }

        // Crear cliente en BD local con auth_id como FK al usuario de Supabase
        Cliente cliente = new Cliente();
        cliente.setAuthId(authId); // FK al UUID de Supabase Auth
        cliente.setNombre(request.nombre());
        cliente.setApellidos(request.apellidos());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setTipoCliente(tipoCliente);
        cliente.setActivo(true);
        cliente.setFechaRegistro(LocalDate.now());

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

    /**
     * Determina el tipo de usuario basándose en la clase concreta
     */
    private String determinarTipoUsuario(Usuario usuario) {
        if (usuario instanceof Cliente) return "cliente";
        if (usuario instanceof Empleado) return "empleado";
        if (usuario instanceof Admin) return "admin";
        return "desconocido";
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
            SupabaseUser usuario = supabaseGateway.obtenerUsuario(accessToken);
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
            return new AuthResponse(
                    accessToken,
                    3600L, // Supabase default: 1 hora
                    "Bearer",
                    authId,
                    usuario.email(),
                    cliente.getNombre(),
                    cliente.getApellidos(),
                    "cliente"
            );

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
            SupabaseUser usuarioSupabase = supabaseGateway.obtenerUsuario(accessToken);
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
            return new AuthResponse(
                    accessToken,
                    3600L, // Tiempo por defecto
                    "Bearer",
                    authId,
                    usuarioSupabase.email(),
                    usuario.getNombre(),
                    usuario.getApellidos(),
                    determinarTipoUsuario(usuario)
            );

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al obtener usuario actual", e);
            throw new NegocioException(TipoError.TOKEN_INVALIDO, "Token de sesión inválido o expirado");
        }
    }

}
