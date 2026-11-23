package com.flystore.modulos.autenticacion.aplicacion;

import com.flystore.contratos.gestionarventa.DatosEntregaDTO;
import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz Business Object para Cliente
 * Expone operaciones de negocio relacionadas con clientes
 *
 * SOLID: DIP - API layer depende de esta abstracción, no de DAOs
 */
public interface IClienteBO {

    /**
     * Busca un cliente por su auth_id de Supabase
     * @param authId UUID del usuario en Supabase Auth
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> findByAuthId(UUID authId);

    /**
     * Obtiene un cliente por su ID interno
     * @param id ID del cliente
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> obtenerPorId(Long id);

    /**
     * ID interno del cliente de la sesión. Los controladores usan este método para no recibir la entidad.
     * @throws com.flystore.dominio.excepcion.NegocioException NO_ENCONTRADO si no es cliente
     */
    Long obtenerIdCliente(UUID authId);

    /**
     * Datos de entrega para proponer en CU-02: los predeterminados, si no los últimos capturados,
     * y si nunca capturó ninguno, su nombre y teléfono de registro.
     */
    DatosEntregaDTO obtenerDatosEntrega(Long clienteId);

    /**
     * Datos de entrega para un pedido. Reutiliza los guardados si son iguales; si no, los guarda.
     * Con guardarComoPredeterminada, quedan como predeterminados del cliente.
     */
    DatosEntrega registrarDatosEntrega(Long clienteId, DatosEntregaDTO datos);

    /**
     * CU-04 · Ids de los clientes cuyo nombre completo contiene el texto o, si el texto son solo
     * dígitos, cuyo teléfono los contiene. La búsqueda de pedidos del personal los usa para
     * encontrar los pedidos de un cliente.
     */
    List<Long> buscarIdsPorNombreOTelefono(String texto);
}
