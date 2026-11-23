package com.flystore.modulos.administrarpedidos.aplicacion.reglas;

import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cobro en efectivo en tienda: lo recibido debe cubrir el total; lo que sobra es el cambio. */
@Component
public class ValidadorPagoEfectivo {

    /** Comprueba que el monto recibido cubra el total y devuelve el cambio. Lanza DATOS_INVALIDOS si no alcanza. */
    public BigDecimal validar(BigDecimal total, BigDecimal montoRecibido) {
        if (montoRecibido == null)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "Captura el monto recibido en efectivo.");
        if (montoRecibido.compareTo(total) < 0)
            throw new NegocioException(TipoError.DATOS_INVALIDOS, "El monto recibido es menor al total del pedido ($"
                    + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + ").");
        return calcularCambio(total, montoRecibido);
    }

    public BigDecimal calcularCambio(BigDecimal total, BigDecimal montoRecibido) {
        return montoRecibido.subtract(total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
