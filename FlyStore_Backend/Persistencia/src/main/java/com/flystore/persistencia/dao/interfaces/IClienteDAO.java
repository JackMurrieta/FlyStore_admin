package com.flystore.persistencia.dao.interfaces;

import com.flystore.persistencia.entidades.Cliente;
import com.flystore.persistencia.entidades.DatosEntrega;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz DAO para Cliente
 * SOLID: ISP - Interfaz pequeña y específica
 * SOLID: DIP - Capa superior depende de abstracción
 */
public interface IClienteDAO {

    /**
     * Busca un cliente por su auth_id de Supabase
     * @param authId UUID del usuario en Supabase Auth
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> findByAuthId(UUID authId);

    /**
     * Guarda un cliente en la base de datos
     * @param cliente Cliente a guardar
     * @return Cliente guardado
     */
    Cliente save(Cliente cliente);

    /**
     * Obtiene un cliente por su ID interno
     * @param id ID del cliente
     * @return Optional con el cliente si existe
     */
    Optional<Cliente> obtenerPorId(Long id);

    /**
     * Obtiene el cliente con bloqueo de escritura hasta el fin de la transacción.
     * Serializa la creación del carrito del mismo cliente.
     */
    Optional<Cliente> bloquearPorId(Long id);

    /**
     * CU-04 · Clientes cuyo nombre completo contiene el texto (sin distinguir mayúsculas) o, si el
     * texto son solo dígitos, cuyo teléfono los contiene.
     */
    List<Cliente> buscarPorNombreOTelefono(String texto);

    /** Persiste unos datos de entrega nuevos del cliente. */
    void guardarDatosEntrega(DatosEntrega datosEntrega);
}
