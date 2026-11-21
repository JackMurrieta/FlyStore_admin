package com.flystore.modulos.perfil.aplicacion;

import com.flystore.contratos.perfil.ActualizarPerfilPeticionDTO;
import com.flystore.contratos.perfil.DireccionEntregaDTO;
import com.flystore.contratos.perfil.PerfilDTO;
import java.util.List;
import java.util.UUID;

/**
 * Gestionar perfil: datos personales del usuario con sesión y direcciones de entrega del cliente.
 * El usuario se identifica con el auth_id de su sesión; nunca con un id que envíe el front.
 * El correo y la contraseña se cambian en IAuthBO (pasan por el proveedor de autenticación).
 */
public interface IPerfilBO {

    /** Perfil del usuario (cliente, empleado o admin). Sincroniza el correo con el del proveedor. */
    PerfilDTO obtenerPerfil(UUID authId, String accessToken);

    /** Actualiza nombre, apellidos y teléfono. */
    PerfilDTO actualizarPerfil(UUID authId, String accessToken, ActualizarPerfilPeticionDTO peticion);

    /** Direcciones activas del cliente; la predeterminada primero. */
    List<DireccionEntregaDTO> listarDirecciones(UUID authId);

    /** Marca la dirección como la que se propone al hacer un pedido. */
    List<DireccionEntregaDTO> marcarPredeterminada(UUID authId, Long idDireccion);

    /** Quita la dirección predeterminada: al pedir se propone la última usada. */
    List<DireccionEntregaDTO> quitarPredeterminada(UUID authId);

    /** Oculta la dirección (activo = false). Los pedidos que ya la usan no cambian. */
    List<DireccionEntregaDTO> eliminarDireccion(UUID authId, Long idDireccion);
}
