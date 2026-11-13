package com.flystore.presentacion.seguridad;

import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import java.util.List;

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
    public SupabaseJwtConverter(IClienteDAO clienteDAO, IEmpleadoDAO empleadoDAO, IAdminDAO adminDAO) {
        this.clienteDAO=clienteDAO; this.empleadoDAO=empleadoDAO; this.adminDAO=adminDAO;
    }
    private List<String> obtenerRoles(UUID id) {
        if (clienteDAO.findByAuthId(id).isPresent()) return List.of("CLIENTE");
        if (empleadoDAO.findByAuthId(id).isPresent()) return List.of("EMPLEADO");
        if (adminDAO.findByAuthId(id).isPresent()) return List.of("ADMIN", "EMPLEADO");
        return List.of("AUTHENTICATED");
    }
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            // Extraer auth_id del JWT (claim "sub")
            UUID authId = UUID.fromString(jwt.getSubject());

            // Buscar usuario en BD local y asignar rol
            Collection<GrantedAuthority> authorities = obtenerRoles(authId).stream()
                    .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol)).toList();

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

}
