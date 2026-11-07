package com.flystore.config;

import com.flystore.entidades.Usuario;
import com.flystore.features.iniciarSesion.DAO.IAdminDAO;
import com.flystore.features.iniciarSesion.DAO.IClienteDAO;
import com.flystore.features.iniciarSesion.DAO.IEmpleadoDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Convierte JWT de Supabase a Authentication de Spring Security
 * SOLID: SRP - Solo convierte JWT a authorities
 * GRASP: Information Expert - Conoce cómo extraer roles del JWT
 */
@Component
public class SupabaseJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseJwtConverter.class);

    private final IClienteDAO clienteDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IAdminDAO adminDAO;

    public SupabaseJwtConverter(IClienteDAO clienteDAO,
                                IEmpleadoDAO empleadoDAO,
                                IAdminDAO adminDAO) {
        this.clienteDAO = clienteDAO;
        this.empleadoDAO = empleadoDAO;
        this.adminDAO = adminDAO;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            // Extraer auth_id del JWT (claim "sub")
            UUID authId = UUID.fromString(jwt.getSubject());

            // Buscar usuario en BD local y asignar rol
            Collection<GrantedAuthority> authorities = extraerAuthorities(authId);

            logger.debug("JWT convertido para usuario: {} con roles: {}", authId, authorities);

            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());

        } catch (Exception e) {
            logger.warn("Error al convertir JWT: {}", e.getMessage());
            // Retornar autenticado sin roles
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        }
    }

    /**
     * Extrae authorities basándose en el tipo de usuario en BD
     */
    private Collection<GrantedAuthority> extraerAuthorities(UUID authId) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Buscar en Cliente
        if (clienteDAO.findByAuthId(authId).isPresent()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));
            logger.debug("Usuario {} es CLIENTE", authId);
            return authorities;
        }

        // Buscar en Empleado
        if (empleadoDAO.findByAuthId(authId).isPresent()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EMPLEADO"));
            logger.debug("Usuario {} es EMPLEADO", authId);
            return authorities;
        }

        // Buscar en Admin
        if (adminDAO.findByAuthId(authId).isPresent()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_EMPLEADO")); // Admin tiene permisos de empleado
            logger.debug("Usuario {} es ADMIN", authId);
            return authorities;
        }

        // Usuario no encontrado en BD local
        logger.warn("Usuario {} no encontrado en BD local", authId);
        authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
        return authorities;
    }
}
