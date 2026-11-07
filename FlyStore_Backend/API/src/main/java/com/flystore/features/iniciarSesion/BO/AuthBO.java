package com.flystore.features.iniciarSesion.BO;

import CU_iniciarSesion_DTOs.AuthResponse;
import CU_iniciarSesion_DTOs.LoginRequest;
import CU_iniciarSesion_DTOs.RegisterRequest;
import CU_iniciarSesion_DTOs.SupabaseSession;
import com.flystore.entidades.Admin;
import com.flystore.entidades.Cliente;
import com.flystore.entidades.Empleado;
import com.flystore.entidades.Usuario;
import com.flystore.excepcion.NegocioException;
import com.flystore.excepcion.TipoError;
import com.flystore.features.iniciarSesion.DAO.IAdminDAO;
import com.flystore.features.iniciarSesion.DAO.IClienteDAO;
import com.flystore.features.iniciarSesion.DAO.IEmpleadoDAO;
import com.flystore.gateway.ISupabaseAuthGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public AuthBO(ISupabaseAuthGateway supabaseGateway,
                  IClienteDAO clienteDAO,
                  IEmpleadoDAO empleadoDAO,
                  IAdminDAO adminDAO) {
        this.supabaseGateway = supabaseGateway;
        this.clienteDAO = clienteDAO;
        this.empleadoDAO = empleadoDAO;
        this.adminDAO = adminDAO;
    }

    @Override
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        logger.info("Registrando usuario: {}", request.email());

        try {
            // 1. Crear metadata para Supabase
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("nombre", request.nombre());
            metadata.put("apellidos", request.apellidos());
            metadata.put("tipo_usuario", request.tipoUsuario());

            // 2. Registrar en Supabase Auth
            SupabaseSession sesion = supabaseGateway.registrar(
                    request.email(),
                    request.password(),
                    metadata
            );

            UUID authId = UUID.fromString(sesion.user().id());

            // 3. Crear usuario en BD local
            Usuario usuario = crearUsuarioLocal(authId, request);

            // 4. Construir respuesta
            return new AuthResponse(
                    sesion.accessToken(),
                    sesion.expiresIn().longValue(),
                    sesion.tokenType(),
                    authId,
                    sesion.user().email(),
                    usuario.getNombre(),
                    usuario.getApellidos(),
                    request.tipoUsuario()
            );

        } catch (NegocioException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al registrar usuario", e);
            throw new NegocioException(TipoError.ERROR_INTERNO, "Error al registrar usuario");
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
     * Crea el usuario en la base de datos local según su tipo
     * GRASP: Creator - AuthBO crea las entidades de usuario
     */
    @Transactional
    private Usuario crearUsuarioLocal(UUID authId, RegisterRequest request) {
        Usuario usuario;

        switch (request.tipoUsuario().toLowerCase()) {
            case "cliente":
                Cliente cliente = new Cliente();
                cliente.setAuthId(authId);
                cliente.setNombre(request.nombre());
                cliente.setApellidos(request.apellidos());
                cliente.setEmail(request.email());
                cliente.setTelefono(request.telefono());
                cliente.setActivo(true);
                cliente.setFechaRegistro(LocalDate.now());
                usuario = clienteDAO.save(cliente);
                break;

            case "empleado":
                Empleado empleado = new Empleado();
                empleado.setAuthId(authId);
                empleado.setNombre(request.nombre());
                empleado.setApellidos(request.apellidos());
                empleado.setEmail(request.email());
                empleado.setTelefono(request.telefono());
                empleado.setActivo(true);
                empleado.setFechaRegistro(LocalDate.now());
                usuario = empleadoDAO.save(empleado);
                break;

            case "admin":
                Admin admin = new Admin();
                admin.setAuthId(authId);
                admin.setNombre(request.nombre());
                admin.setApellidos(request.apellidos());
                admin.setEmail(request.email());
                admin.setTelefono(request.telefono());
                admin.setActivo(true);
                admin.setFechaRegistro(LocalDate.now());
                usuario = adminDAO.save(admin);
                break;

            default:
                throw new NegocioException(
                        TipoError.DATOS_INVALIDOS,
                        "Tipo de usuario no válido: " + request.tipoUsuario()
                );
        }

        logger.info("Usuario creado en BD local: {}", authId);
        return usuario;
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
}
