package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.contratos.gestionarventa.DatosEntregaDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.persistencia.dao.interfaces.IClienteDAO;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Business Object para Cliente
 * Implementa lógica de negocio relacionada con clientes
 *
 * SOLID: SRP - Responsable únicamente de operaciones de negocio de clientes
 * SOLID: DIP - Depende de abstracción IClienteDAO
 */
@Service
@Transactional(readOnly = true)
public class ClienteBO implements IClienteBO {

    private final IClienteDAO clienteDAO;
    private final PedidoVentaMapper mapper;

    public ClienteBO(IClienteDAO clienteDAO, PedidoVentaMapper mapper) {
        this.clienteDAO = clienteDAO;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cliente> findByAuthId(UUID authId) {
        return clienteDAO.findByAuthId(authId);
    }

    @Override
    public Optional<Cliente> obtenerPorId(Long id) {
        return clienteDAO.obtenerPorId(id);
    }

    @Override
    public Long obtenerIdCliente(UUID authId) {
        return clienteDAO.findByAuthId(authId)
                .map(Cliente::getId)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Cliente no encontrado"));
    }

    @Override
    public DatosEntregaDTO obtenerDatosEntrega(Long clienteId) {
        var cliente = cliente(clienteId);
        if (cliente.getDireccionPredeterminada() != null)
            return mapper.aDatosEntregaDTO(cliente.getDireccionPredeterminada(), true);
        return activos(cliente).max(Comparator.comparing(DatosEntrega::getId))
                .map(d -> mapper.aDatosEntregaDTO(d, false))
                .orElseGet(() -> mapper.aDatosEntregaDTO(cliente));
    }

    @Override
    @Transactional
    public DatosEntrega registrarDatosEntrega(Long clienteId, DatosEntregaDTO datos) {
        var cliente = cliente(clienteId);
        var nuevos = mapper.aDatosEntrega(datos);
        var guardados = Optional.ofNullable(cliente.getDireccionPredeterminada())
                .filter(d -> iguales(d, nuevos))
                .or(() -> activos(cliente).filter(d -> iguales(d, nuevos)).findFirst());
        var resultado = guardados.orElseGet(() -> {
            nuevos.setCliente(cliente);
            clienteDAO.guardarDatosEntrega(nuevos);
            return nuevos;
        });
        if (datos.guardarComoPredeterminada()) cliente.setDireccionPredeterminada(resultado);
        return resultado;
    }

    private Cliente cliente(Long clienteId) {
        return clienteDAO.obtenerPorId(clienteId)
                .orElseThrow(() -> new NegocioException(TipoError.NO_ENCONTRADO, "Cliente no encontrado"));
    }

    private static java.util.stream.Stream<DatosEntrega> activos(Cliente cliente) {
        return cliente.getDatosEntrega() == null ? java.util.stream.Stream.empty()
                : cliente.getDatosEntrega().stream().filter(d -> !Boolean.FALSE.equals(d.getActivo()));
    }

    private static boolean iguales(DatosEntrega a, DatosEntrega b) {
        return Objects.equals(a.getNombreCompleto(), b.getNombreCompleto())
                && Objects.equals(a.getTelefono(), b.getTelefono())
                && Objects.equals(a.getCiudad(), b.getCiudad())
                && Objects.equals(a.getEstado(), b.getEstado())
                && Objects.equals(a.getDireccion(), b.getDireccion())
                && Objects.equals(a.getReferencias(), b.getReferencias());
    }
}
