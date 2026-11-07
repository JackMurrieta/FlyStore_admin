package com.flystore.features.iniciarSesion.service;

import CU_iniciarSesion_DTOs.AuthResponseDTO;
import CU_iniciarSesion_DTOs.LoginRequestDTO;
import CU_iniciarSesion_DTOs.RegisterRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flystore.config.SupabaseProperties;
import com.flystore.persistencia.entidades.Admin;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.Empleado;
import com.flystore.persistencia.entidades.Usuario;
import com.flystore.features.iniciarSesion.repository.AdminRepository;
import com.flystore.features.iniciarSesion.repository.ClienteRepository;
import com.flystore.features.iniciarSesion.repository.EmpleadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de autenticación que actúa como wrapper de Supabase Auth
 * Feature: Iniciar Sesión
 * Maneja login, registro y sincronización con la base de datos local
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private SupabaseProperties supabaseProperties;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private AdminRepository adminRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Inicia sesión de usuario usando Supabase Auth API
     */
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            logger.info("Iniciando sesión para usuario: {}", loginRequest.getEmail());

            // Llamar a Supabase Auth API
            String authUrl = supabaseProperties.getUrl() + "/auth/v1/token?grant_type=password";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", getSupabaseAnonKey());

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("email", loginRequest.getEmail());
            requestBody.put("password", loginRequest.getPassword());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                // Extraer datos del response
                String accessToken = jsonResponse.get("access_token").asText();
                String refreshToken = jsonResponse.get("refresh_token").asText();
                Long expiresIn = jsonResponse.get("expires_in").asLong();

                JsonNode userNode = jsonResponse.get("user");
                UUID userId = UUID.fromString(userNode.get("id").asText());
                String email = userNode.get("email").asText();

                // Buscar el usuario en nuestra BD
                Usuario usuario = findUsuarioByAuthId(userId);

                if (usuario == null) {
                    logger.warn("Usuario con auth_id {} no encontrado en BD local", userId);
                    throw new RuntimeException("Usuario no encontrado en la base de datos");
                }

                // Construir respuesta
                AuthResponseDTO authResponse = new AuthResponseDTO();
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
                authResponse.setExpiresIn(expiresIn);
                authResponse.setTokenType("Bearer");
                authResponse.setUserId(userId);
                authResponse.setEmail(email);
                authResponse.setNombre(usuario.getNombre());
                authResponse.setApellidos(usuario.getApellidos());
                authResponse.setTipoUsuario(determinarTipoUsuario(usuario));

                logger.info("Sesión iniciada exitosamente para: {}", email);
                return authResponse;
            }

            throw new RuntimeException("Error al iniciar sesión en Supabase");

        } catch (Exception e) {
            logger.error("Error al iniciar sesión", e);
            throw new RuntimeException("Error al iniciar sesión: " + e.getMessage());
        }
    }

    /**
     * Registra un nuevo usuario en Supabase Auth y en la BD local
     */
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO registerRequest) {
        try {
            logger.info("Registrando nuevo usuario: {}", registerRequest.getEmail());

            // 1. Crear usuario en Supabase Auth
            String authUrl = supabaseProperties.getUrl() + "/auth/v1/signup";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", getSupabaseAnonKey());

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("email", registerRequest.getEmail());
            requestBody.put("password", registerRequest.getPassword());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());

                // Extraer datos del response
                String accessToken = jsonResponse.get("access_token").asText();
                String refreshToken = jsonResponse.get("refresh_token").asText();
                Long expiresIn = jsonResponse.get("expires_in").asLong();

                JsonNode userNode = jsonResponse.get("user");
                UUID userId = UUID.fromString(userNode.get("id").asText());
                String email = userNode.get("email").asText();

                // 2. Crear usuario en nuestra BD local
                Usuario usuario = crearUsuarioEnBD(userId, registerRequest);

                // 3. Construir respuesta
                AuthResponseDTO authResponse = new AuthResponseDTO();
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
                authResponse.setExpiresIn(expiresIn);
                authResponse.setTokenType("Bearer");
                authResponse.setUserId(userId);
                authResponse.setEmail(email);
                authResponse.setNombre(usuario.getNombre());
                authResponse.setApellidos(usuario.getApellidos());
                authResponse.setTipoUsuario(registerRequest.getTipoUsuario());

                logger.info("Usuario registrado exitosamente: {}", email);
                return authResponse;
            }

            throw new RuntimeException("Error al registrar usuario en Supabase");

        } catch (Exception e) {
            logger.error("Error al registrar usuario", e);
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage());
        }
    }

    /**
     * Crea el usuario en la base de datos local según su tipo
     */
    private Usuario crearUsuarioEnBD(UUID authId, RegisterRequestDTO registerRequest) {
        Usuario usuario;

        switch (registerRequest.getTipoUsuario().toLowerCase()) {
            case "cliente":
                Cliente cliente = new Cliente();
                cliente.setAuthId(authId);
                cliente.setNombre(registerRequest.getNombre());
                cliente.setApellidos(registerRequest.getApellidos());
                cliente.setEmail(registerRequest.getEmail());
                cliente.setTelefono(registerRequest.getTelefono());
                cliente.setActivo(true);
                cliente.setFechaRegistro(LocalDate.now());
                usuario = clienteRepository.save(cliente);
                break;

            case "empleado":
                Empleado empleado = new Empleado();
                empleado.setAuthId(authId);
                empleado.setNombre(registerRequest.getNombre());
                empleado.setApellidos(registerRequest.getApellidos());
                empleado.setEmail(registerRequest.getEmail());
                empleado.setTelefono(registerRequest.getTelefono());
                empleado.setActivo(true);
                empleado.setFechaRegistro(LocalDate.now());
                usuario = empleadoRepository.save(empleado);
                break;

            case "admin":
                Admin admin = new Admin();
                admin.setAuthId(authId);
                admin.setNombre(registerRequest.getNombre());
                admin.setApellidos(registerRequest.getApellidos());
                admin.setEmail(registerRequest.getEmail());
                admin.setTelefono(registerRequest.getTelefono());
                admin.setActivo(true);
                admin.setFechaRegistro(LocalDate.now());
                usuario = adminRepository.save(admin);
                break;

            default:
                throw new IllegalArgumentException("Tipo de usuario no válido: " + registerRequest.getTipoUsuario());
        }

        logger.info("Usuario creado en BD local con ID: {} y auth_id: {}", usuario.getId(), authId);
        return usuario;
    }

    /**
     * Busca un usuario por su auth_id en todas las tablas de usuarios
     */
    private Usuario findUsuarioByAuthId(UUID authId) {
        // Buscar en clientes
        Usuario usuario = clienteRepository.findByAuthId(authId).orElse(null);
        if (usuario != null) return usuario;

        // Buscar en empleados
        usuario = empleadoRepository.findByAuthId(authId).orElse(null);
        if (usuario != null) return usuario;

        // Buscar en admins
        usuario = adminRepository.findByAuthId(authId).orElse(null);
        return usuario;
    }

    /**
     * Determina el tipo de usuario (cliente, empleado, admin)
     */
    private String determinarTipoUsuario(Usuario usuario) {
        if (usuario instanceof Cliente) return "cliente";
        if (usuario instanceof Empleado) return "empleado";
        if (usuario instanceof Admin) return "admin";
        return "desconocido";
    }

    /**
     * Obtiene la Publishable Key de Supabase desde application.properties
     * Esta key se usa para operaciones públicas como login y register
     */
    private String getSupabaseAnonKey() {
        String publishableKey = supabaseProperties.getPublishableKey();

        if (publishableKey == null || publishableKey.isEmpty() || publishableKey.equals("REEMPLAZAR_CON_TU_PUBLISHABLE_KEY")) {
            throw new IllegalStateException(
                "Supabase Publishable Key no está configurada. " +
                "Ve a tu proyecto en Supabase Dashboard → Settings → API → Publishable Key " +
                "y actualiza application.properties: supabase.publishable-key=TU_PUBLISHABLE_KEY"
            );
        }

        return publishableKey;
    }
}
