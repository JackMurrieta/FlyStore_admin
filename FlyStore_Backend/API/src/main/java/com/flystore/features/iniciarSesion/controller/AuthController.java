package com.flystore.features.iniciarSesion.controller;

import CU_iniciarSesion_DTOs.AuthResponseDTO;
import CU_iniciarSesion_DTOs.LoginRequestDTO;
import CU_iniciarSesion_DTOs.RegisterRequestDTO;
import com.flystore.features.iniciarSesion.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para autenticación de usuarios
 * Feature: Iniciar Sesión
 * Actúa como wrapper de Supabase Auth API
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")  // TODO: Configurar CORS apropiadamente en producción
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/login
     * Inicia sesión de usuario
     *
     * @param loginRequest Credenciales de usuario (email y password)
     * @return Token JWT y datos del usuario
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                    BindingResult bindingResult) {
        try {
            // Validar errores de validación
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = bindingResult.getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                error -> error.getField(),
                                error -> error.getDefaultMessage()
                        ));
                return ResponseEntity.badRequest().body(errors);
            }

            logger.info("Solicitud de login para: {}", loginRequest.getEmail());

            AuthResponseDTO response = authService.login(loginRequest);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en login", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Credenciales inválidas");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    /**
     * POST /api/auth/register
     * Registra un nuevo usuario
     *
     * @param registerRequest Datos del nuevo usuario
     * @return Token JWT y datos del usuario creado
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest,
                                       BindingResult bindingResult) {
        try {
            // Validar errores de validación
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = bindingResult.getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                error -> error.getField(),
                                error -> error.getDefaultMessage()
                        ));
                return ResponseEntity.badRequest().body(errors);
            }

            logger.info("Solicitud de registro para: {}", registerRequest.getEmail());

            AuthResponseDTO response = authService.register(registerRequest);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error en registro", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al registrar usuario");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * GET /api/auth/health
     * Endpoint para verificar que el servicio de auth está funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Auth Service");
        return ResponseEntity.ok(response);
    }
}
