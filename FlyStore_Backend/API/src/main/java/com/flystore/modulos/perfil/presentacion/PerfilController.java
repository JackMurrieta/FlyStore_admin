package com.flystore.modulos.perfil.presentacion;

import com.flystore.compartido.presentacion.CookieUtils;
import com.flystore.contratos.perfil.*;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.IAuthBO;
import com.flystore.modulos.autenticacion.aplicacion.modelo.AuthResponse;
import com.flystore.modulos.perfil.aplicacion.IPerfilBO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestionar perfil (/api/perfil): datos personales, direcciones de entrega y seguridad de la cuenta.
 * El usuario sale de la sesión (cookie con el JWT de Supabase); el front nunca envía su id.
 */
@RestController
@RequestMapping("/api/perfil")
@PreAuthorize("isAuthenticated()")
public class PerfilController {

    /** Minutos que dura la sesión abierta por el enlace de recuperación para escribir la nueva contraseña. */
    private static final long MINUTOS_RECUPERACION = 15;

    private final IPerfilBO perfilBO;
    private final IAuthBO authBO;
    private final CookieUtils cookieUtils;

    public PerfilController(IPerfilBO perfilBO, IAuthBO authBO, CookieUtils cookieUtils) {
        this.perfilBO = perfilBO;
        this.authBO = authBO;
        this.cookieUtils = cookieUtils;
    }

    // ===== Datos personales =====

    @GetMapping
    public ResponseEntity<PerfilDTO> obtenerPerfil(Authentication sesion, HttpServletRequest request) {
        return ResponseEntity.ok(perfilBO.obtenerPerfil(authId(sesion), accessToken(request)));
    }

    @PutMapping
    public ResponseEntity<PerfilDTO> actualizarPerfil(@Valid @RequestBody ActualizarPerfilPeticionDTO peticion,
                                                      Authentication sesion, HttpServletRequest request) {
        return ResponseEntity.ok(perfilBO.actualizarPerfil(authId(sesion), accessToken(request), peticion));
    }

    // ===== Direcciones de entrega (solo clientes) =====

    @GetMapping("/direcciones")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<DireccionEntregaDTO>> listarDirecciones(Authentication sesion) {
        return ResponseEntity.ok(perfilBO.listarDirecciones(authId(sesion)));
    }

    @PutMapping("/direcciones/{id}/predeterminada")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<DireccionEntregaDTO>> marcarPredeterminada(@PathVariable Long id, Authentication sesion) {
        return ResponseEntity.ok(perfilBO.marcarPredeterminada(authId(sesion), id));
    }

    @DeleteMapping("/direcciones/predeterminada")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<DireccionEntregaDTO>> quitarPredeterminada(Authentication sesion) {
        return ResponseEntity.ok(perfilBO.quitarPredeterminada(authId(sesion)));
    }

    @DeleteMapping("/direcciones/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<DireccionEntregaDTO>> eliminarDireccion(@PathVariable Long id, Authentication sesion) {
        return ResponseEntity.ok(perfilBO.eliminarDireccion(authId(sesion), id));
    }

    // ===== Seguridad de la cuenta =====

    /** Pide el cambio de correo; Supabase envía el enlace de confirmación. */
    @PostMapping("/correo")
    public ResponseEntity<MensajeDTO> cambiarCorreo(@Valid @RequestBody CambiarCorreoPeticionDTO peticion,
                                                    HttpServletRequest request) {
        authBO.solicitarCambioCorreo(accessToken(request), peticion.correo());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new MensajeDTO(
                "Te enviamos un enlace a " + peticion.correo().trim() + ". Tu correo cambiará cuando lo confirmes."));
    }

    /** Cambia la contraseña confirmando la actual; renueva las cookies con la sesión verificada. */
    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambiarContrasenaPeticionDTO peticion,
                                                  HttpServletRequest request, HttpServletResponse response) {
        AuthResponse sesion = authBO.cambiarContrasena(accessToken(request), peticion.actual(), peticion.nueva());
        cookieUtils.setAccessTokenCookie(response, sesion.accessToken(), sesion.expiresIn());
        if (sesion.refreshToken() != null && !sesion.refreshToken().isBlank())
            cookieUtils.setRefreshTokenCookie(response, sesion.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Nueva contraseña tras abrir el enlace de recuperación (la sesión debe venir de ese enlace). */
    @PostMapping("/contrasena/restablecer")
    public ResponseEntity<Void> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaPeticionDTO peticion,
                                                      Authentication sesion, HttpServletRequest request) {
        authBO.restablecerContrasena(accessToken(request), recuperacionReciente(sesion), peticion.nueva());
        return ResponseEntity.noContent().build();
    }

    // ===== Auxiliares =====

    private static UUID authId(Authentication sesion) {
        return UUID.fromString(sesion.getName());
    }

    private String accessToken(HttpServletRequest request) {
        return cookieUtils.getAccessTokenFromCookies(request)
                .orElseThrow(() -> new NegocioException(TipoError.TOKEN_INVALIDO, "No se encontró sesión activa"));
    }

    /**
     * El JWT de Supabase registra cómo se abrió la sesión en el claim amr: [{method, timestamp}].
     * Solo se acepta «recovery» de los últimos minutos.
     */
    static boolean recuperacionReciente(Authentication sesion) {
        if (!(sesion instanceof JwtAuthenticationToken token)) return false;
        Object amr = token.getToken().getClaims().get("amr");
        if (!(amr instanceof List<?> metodos)) return false;
        long limite = Instant.now().getEpochSecond() - MINUTOS_RECUPERACION * 60;
        return metodos.stream().anyMatch(m -> m instanceof Map<?, ?> metodo
                && "recovery".equals(metodo.get("method"))
                && metodo.get("timestamp") instanceof Number marca && marca.longValue() >= limite);
    }
}
