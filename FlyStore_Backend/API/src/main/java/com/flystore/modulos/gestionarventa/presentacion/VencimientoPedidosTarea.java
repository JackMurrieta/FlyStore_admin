package com.flystore.modulos.gestionarventa.presentacion;

import com.flystore.modulos.gestionarventa.aplicacion.IPedidoVentaBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Regla de las 42 h: revisa periódicamente los pedidos registrados sin pago y cancela por
 * vencimiento los que superaron el plazo. Es un adaptador de entrada, como un controlador.
 */
@Component
public class VencimientoPedidosTarea {

    private static final Logger logger = LoggerFactory.getLogger(VencimientoPedidosTarea.class);

    private final IPedidoVentaBO pedidoVentaBO;

    public VencimientoPedidosTarea(IPedidoVentaBO pedidoVentaBO) {
        this.pedidoVentaBO = pedidoVentaBO;
    }

    @Scheduled(initialDelayString = "${app.ventas.vencimiento.retraso-inicial-ms:60000}",
               fixedDelayString = "${app.ventas.vencimiento.intervalo-ms:900000}")
    public void cancelarPedidosVencidos() {
        try {
            int cancelados = pedidoVentaBO.cancelarPedidosVencidos();
            if (cancelados > 0) logger.info("Pedidos cancelados por vencimiento: {}", cancelados);
        } catch (RuntimeException e) {
            // Un fallo puntual (por ejemplo, la base no disponible) no debe detener las siguientes revisiones.
            logger.error("No se pudo revisar el vencimiento de pedidos", e);
        }
    }
}
