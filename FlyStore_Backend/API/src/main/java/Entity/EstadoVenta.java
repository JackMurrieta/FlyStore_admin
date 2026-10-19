package Entity;

/**
 * Enum EstadoVenta - Estados del pedido de venta
 * @author Usuario
 */
public enum EstadoVenta {
    PENDIENTE("pendiente"),
    AGUARDANDO_PENDIENTE_PAGO("aguardando_pendiente_pago"),
    PENDIENTE_COMPROBANTE("pendiente_comprobante"),
    PAGO_EN_REVISION("pago_en_revision"),
    PAGADO("pagado"),
    PREPARACION("preparacion"),
    ENVIADO("enviado"),
    ENTREGADO("entregado"),
    CANCELADO("cancelado");

    private final String valor;

    EstadoVenta(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static EstadoVenta fromValor(String valor) {
        for (EstadoVenta estado : EstadoVenta.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de venta no válido: " + valor);
    }
}
