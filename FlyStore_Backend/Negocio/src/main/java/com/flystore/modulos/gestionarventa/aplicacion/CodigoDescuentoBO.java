package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.persistencia.dao.interfaces.ICodigoDescuentoDAO;
import com.flystore.persistencia.entidades.CodigoDescuento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Validación y conteo de usos de los códigos de descuento. */
@Service
public class CodigoDescuentoBO implements ICodigoDescuentoBO {

    static final String CODIGO_NO_VALIDO = "El código no es válido";

    private final ICodigoDescuentoDAO codigoDescuentoDAO;
    private final Clock reloj;

    public CodigoDescuentoBO(ICodigoDescuentoDAO codigoDescuentoDAO, Clock reloj) {
        this.codigoDescuentoDAO = codigoDescuentoDAO;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public CodigoDescuento validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new NegocioException(TipoError.DATOS_INVALIDOS, "Escribe un código");
        var encontrado = codigoDescuentoDAO.obtenerPorCodigo(codigo.trim())
                .orElseThrow(() -> new NegocioException(TipoError.DATOS_INVALIDOS, CODIGO_NO_VALIDO));
        validarVigenciaYUsos(encontrado, LocalDate.now(reloj));
        return encontrado;
    }

    @Override
    public void validarVigenciaYUsos(CodigoDescuento codigo, LocalDate hoy) {
        if (!Boolean.TRUE.equals(codigo.getActivo()) || codigo.getMontoFijo() == null
                || codigo.getMontoFijo().compareTo(BigDecimal.ZERO) <= 0)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, CODIGO_NO_VALIDO);
        if (codigo.getFechaInicio() != null && hoy.isBefore(codigo.getFechaInicio()))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El código todavía no está vigente");
        if (codigo.getFechaFin() != null && hoy.isAfter(codigo.getFechaFin()))
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El código ya venció");
        if (codigo.getLimiteUsos() != null && usos(codigo) >= codigo.getLimiteUsos())
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El código ya alcanzó su límite de usos");
    }

    @Override
    public boolean esVigente(CodigoDescuento codigo, LocalDate hoy) {
        try {
            validarVigenciaYUsos(codigo, hoy);
            return true;
        } catch (NegocioException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> obtenerProductosAplicables(CodigoDescuento codigo) {
        return new HashSet<>(codigoDescuentoDAO.obtenerIdsProductosAplicables(codigo.getId()));
    }

    @Override
    @Transactional
    public void registrarUso(Collection<CodigoDescuento> codigos) {
        var hoy = LocalDate.now(reloj);
        // Orden fijo de bloqueo para que dos registros simultáneos no se bloqueen mutuamente.
        for (var id : ids(codigos)) {
            var codigo = codigoDescuentoDAO.bloquearPorId(id)
                    .orElseThrow(() -> new NegocioException(TipoError.DATOS_INVALIDOS, CODIGO_NO_VALIDO));
            try {
                validarVigenciaYUsos(codigo, hoy);
            } catch (NegocioException e) {
                throw new NegocioException(TipoError.DATOS_INVALIDOS,
                        "El código " + codigo.getCodigo() + " ya no se puede usar. Vuelve al carrito para ver el total actualizado.");
            }
            codigo.setUsosActuales(usos(codigo) + 1);
        }
    }

    @Override
    @Transactional
    public void liberarUso(Collection<CodigoDescuento> codigos) {
        for (var id : ids(codigos)) {
            codigoDescuentoDAO.bloquearPorId(id).ifPresent(codigo -> codigo.setUsosActuales(Math.max(0, usos(codigo) - 1)));
        }
    }

    private static int usos(CodigoDescuento codigo) {
        return codigo.getUsosActuales() == null ? 0 : codigo.getUsosActuales();
    }

    private static Set<Long> ids(Collection<CodigoDescuento> codigos) {
        var ids = new TreeSet<Long>();
        codigos.stream().filter(Objects::nonNull).map(CodigoDescuento::getId).filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }
}
