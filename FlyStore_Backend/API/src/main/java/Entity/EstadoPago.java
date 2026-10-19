package Entity;

/**
 * Enum EstadoPago - Estados del pago
 * @author Usuario
 */
public enum EstadoPago {
    PENDIENTE("pendiente"),
    EN_PROCESO("en_proceso"),
    COMPLETADO("completado");

    private final String valor;

    EstadoPago(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static EstadoPago fromValor(String valor) {
        for (EstadoPago estado : EstadoPago.values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pago no válido: " + valor);
    }
}
