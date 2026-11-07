package com.flystore.features.iniciarSesion.controller;

import CU_iniciarSesion_DTOs.AuthResponse;
import CU_iniciarSesion_DTOs.LoginRequest;
import CU_iniciarSesion_DTOs.RegisterRequest;
import com.flystore.features.iniciarSesion.BO.IAuthBO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para caso de uso: Iniciar Sesión
 * SOLID: SRP - Solo maneja HTTP (entrada/salida, códigos de respuesta)
 * GRASP: Controller - Coordina el caso de uso de autenticación
 * SOLID: DIP - Depende de IAuthBO (abstracción), no de implementación
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final IAuthBO authBO;

    // SOLID: Inyección por CONSTRUCTOR (no @Autowired en campos)
    public AuthController(IAuthBO authBO) {
        this.authBO = authBO;
    }

    /**
     * POST /api/auth/login
     * Inicia sesión de usuario
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("POST /api/auth/login - email: {}", request.email());

        AuthResponse response = authBO.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/registro
     * Registra un nuevo usuario
     */
    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegisterRequest request) {
        logger.info("POST /api/auth/registro - email: {}", request.email());

        AuthResponse response = authBO.registrar(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/refresh
     * Refresca el access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody String refreshToken) {
        logger.info("POST /api/auth/refresh");

        AuthResponse response = authBO.refrescarToken(refreshToken);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Cierra la sesión del usuario
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/auth/logout");

        String accessToken = authHeader.replace("Bearer ", "");
        authBO.cerrarSesion(accessToken);

        return ResponseEntity.noContent().build();
    }
}
