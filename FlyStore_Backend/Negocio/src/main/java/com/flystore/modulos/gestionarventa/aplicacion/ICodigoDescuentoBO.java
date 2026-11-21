package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.persistencia.entidades.CodigoDescuento;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

/**
 * Reglas de los códigos de descuento. Un código resta su monto fijo a cada pieza de los productos
 * ligados a él; su uso se cuenta una vez por pedido registrado.
 */
public interface ICodigoDescuentoBO {

    /** Busca el código y comprueba que esté activo, vigente y con usos disponibles. */
    CodigoDescuento validarCodigo(String codigo);

    /** Lanza NegocioException si el código no está activo, vigente o ya agotó sus usos. */
    void validarVigenciaYUsos(CodigoDescuento codigo, LocalDate hoy);

    /** Igual que validarVigenciaYUsos, sin lanzar excepción. */
    boolean esVigente(CodigoDescuento codigo, LocalDate hoy);

    /** IDs de los productos a los que aplica el código. */
    Set<Long> obtenerProductosAplicables(CodigoDescuento codigo);

    /** Cuenta un uso de cada código al registrar un pedido; vuelve a validar con bloqueo. */
    void registrarUso(Collection<CodigoDescuento> codigos);

    /** Devuelve el uso de cada código cuando un pedido registrado vence sin pago. */
    void liberarUso(Collection<CodigoDescuento> codigos);
}
