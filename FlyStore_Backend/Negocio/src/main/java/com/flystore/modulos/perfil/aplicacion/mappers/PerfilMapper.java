package com.flystore.modulos.perfil.aplicacion.mappers;

import com.flystore.contratos.perfil.DireccionEntregaDTO;
import com.flystore.contratos.perfil.PerfilDTO;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;
import com.flystore.persistencia.entidades.Admin;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;
import com.flystore.persistencia.entidades.Empleado;
import com.flystore.persistencia.entidades.Usuario;
import org.springframework.stereotype.Component;

/** Convierte entidades del usuario y sus direcciones en los DTO del perfil. */
@Component
public class PerfilMapper {

    public PerfilDTO aPerfilDTO(Usuario usuario, IdentidadAutenticada identidad) {
        String tipoCliente = usuario instanceof Cliente c && c.getTipoCliente() != null ? c.getTipoCliente().name() : null;
        return new PerfilDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidos() == null ? "" : usuario.getApellidos(),
                identidad.email() != null ? identidad.email() : usuario.getEmail(),
                usuario.getTelefono(),
                tipoUsuario(usuario),
                tipoCliente,
                usuario.getFechaRegistro(),
                identidad.proveedores(),
                identidad.tieneContrasena(),
                identidad.nuevoCorreo());
    }

    public DireccionEntregaDTO aDireccionDTO(DatosEntrega datos, boolean predeterminada) {
        return new DireccionEntregaDTO(datos.getId(), datos.getNombreCompleto(), datos.getTelefono(), datos.getCiudad(),
                datos.getEstado(), datos.getDireccion(), datos.getReferencias(), predeterminada);
    }

    public static String tipoUsuario(Usuario usuario) {
        if (usuario instanceof Cliente) return "cliente";
        if (usuario instanceof Admin) return "admin";
        if (usuario instanceof Empleado) return "empleado";
        return "desconocido";
    }
}
