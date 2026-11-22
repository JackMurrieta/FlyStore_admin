package com.flystore.modulos.perfil.aplicacion;

import com.flystore.contratos.perfil.ActualizarPerfilPeticionDTO;
import com.flystore.contratos.perfil.DireccionEntregaDTO;
import com.flystore.contratos.perfil.PerfilDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.autenticacion.aplicacion.modelo.IdentidadAutenticada;
import com.flystore.modulos.autenticacion.aplicacion.puertos.IProveedorAutenticacion;
import com.flystore.modulos.perfil.aplicacion.mappers.PerfilMapper;
import com.flystore.persistencia.dao.interfaces.IAdminDAO;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.dao.interfaces.IEmpleadoDAO;
import com.flystore.persistencia.entidades.Admin;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;
import com.flystore.persistencia.entidades.Empleado;
import com.flystore.persistencia.entidades.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * BO del caso Gestionar perfil. Lee y escribe al usuario con los DAO de cada rol
 * (como AuthBO) y las direcciones a través del Cliente (relación datos_entrega).
 */
@Service
public class PerfilBO implements IPerfilBO {

    private final IClienteDAO clienteDAO;
    private final IEmpleadoDAO empleadoDAO;
    private final IAdminDAO adminDAO;
    private final IProveedorAutenticacion proveedor;
    private final PerfilMapper mapper;

    public PerfilBO(IClienteDAO clienteDAO, IEmpleadoDAO empleadoDAO, IAdminDAO adminDAO,
                    IProveedorAutenticacion proveedor, PerfilMapper mapper) {
        this.clienteDAO = clienteDAO;
        this.empleadoDAO = empleadoDAO;
        this.adminDAO = adminDAO;
        this.proveedor = proveedor;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PerfilDTO obtenerPerfil(UUID authId, String accessToken) {
        Usuario usuario = usuario(authId);
        IdentidadAutenticada identidad = proveedor.obtenerUsuario(accessToken);
        if (!authId.toString().equals(identidad.id()))
            throw new NegocioException(TipoError.ACCESO_DENEGADO, "La sesión no corresponde a este usuario");
        // Supabase es la fuente del correo: si cambió (enlace confirmado), se copia al usuario local.
        if (identidad.email() != null && !identidad.email().equalsIgnoreCase(usuario.getEmail())) {
            usuario.setEmail(identidad.email());
            guardar(usuario);
        }
        return mapper.aPerfilDTO(usuario, identidad);
    }

    @Override
    @Transactional
    public PerfilDTO actualizarPerfil(UUID authId, String accessToken, ActualizarPerfilPeticionDTO peticion) {
        Usuario usuario = usuario(authId);
        usuario.setNombre(peticion.nombre().trim());
        usuario.setApellidos(peticion.apellidos().trim());
        String telefono = peticion.telefono() == null ? "" : peticion.telefono().replaceAll("\\D", "");
        usuario.setTelefono(telefono.isEmpty() ? null : telefono);
        guardar(usuario);
        return obtenerPerfil(authId, accessToken);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DireccionEntregaDTO> listarDirecciones(UUID authId) {
        return direcciones(cliente(authId));
    }

    @Override
    @Transactional
    public List<DireccionEntregaDTO> marcarPredeterminada(UUID authId, Long idDireccion) {
        Cliente cliente = cliente(authId);
        cliente.setDireccionPredeterminada(direccionActiva(cliente, idDireccion));
        clienteDAO.save(cliente);
        return direcciones(cliente);
    }

    @Override
    @Transactional
    public List<DireccionEntregaDTO> quitarPredeterminada(UUID authId) {
        Cliente cliente = cliente(authId);
        cliente.setDireccionPredeterminada(null);
        clienteDAO.save(cliente);
        return direcciones(cliente);
    }

    @Override
    @Transactional
    public List<DireccionEntregaDTO> eliminarDireccion(UUID authId, Long idDireccion) {
        Cliente cliente = cliente(authId);
        DatosEntrega direccion = direccionActiva(cliente, idDireccion);
        direccion.setActivo(false);
        if (esPredeterminada(cliente, direccion)) cliente.setDireccionPredeterminada(null);
        clienteDAO.save(cliente);
        return direcciones(cliente);
    }

    // ===== Auxiliares =====

    private List<DireccionEntregaDTO> direcciones(Cliente cliente) {
        List<DatosEntrega> lista = cliente.getDatosEntrega() == null ? List.of() : cliente.getDatosEntrega();
        return lista.stream()
                .filter(d -> !Boolean.FALSE.equals(d.getActivo()))
                .sorted(Comparator.comparing((DatosEntrega d) -> !esPredeterminada(cliente, d))
                        .thenComparing(DatosEntrega::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(d -> mapper.aDireccionDTO(d, esPredeterminada(cliente, d)))
                .toList();
    }

    private static boolean esPredeterminada(Cliente cliente, DatosEntrega datos) {
        DatosEntrega predeterminada = cliente.getDireccionPredeterminada();
        return predeterminada != null && Objects.equals(predeterminada.getId(), datos.getId());
    }

    private DatosEntrega direccionActiva(Cliente cliente, Long idDireccion) {
        List<DatosEntrega> lista = cliente.getDatosEntrega() == null ? List.of() : cliente.getDatosEntrega();
        return lista.stream()
                .filter(d -> Objects.equals(d.getId(), idDireccion) && !Boolean.FALSE.equals(d.getActivo()))
                .findFirst()
                // Una dirección ajena responde igual que una inexistente.
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "No encontramos esa dirección"));
    }

    private Cliente cliente(UUID authId) {
        return clienteDAO.findByAuthId(authId)
                .orElseThrow(() -> new NegocioException(TipoError.ACCESO_DENEGADO, "Solo los clientes tienen direcciones de entrega"));
    }

    private Usuario usuario(UUID authId) {
        return clienteDAO.findByAuthId(authId).map(u -> (Usuario) u)
                .or(() -> empleadoDAO.findByAuthId(authId).map(u -> (Usuario) u))
                .or(() -> adminDAO.findByAuthId(authId).map(u -> (Usuario) u))
                .orElseThrow(() -> new NegocioException(TipoError.USUARIO_NO_ENCONTRADO, "Usuario no registrado en el sistema"));
    }

    private void guardar(Usuario usuario) {
        if (usuario instanceof Cliente c) clienteDAO.save(c);
        else if (usuario instanceof Admin a) adminDAO.save(a);
        else if (usuario instanceof Empleado e) empleadoDAO.save(e);
    }
}
