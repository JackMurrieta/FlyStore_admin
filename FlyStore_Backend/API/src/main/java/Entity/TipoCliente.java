package Entity;

/**
 * Enum TipoCliente - Define los tipos de cliente según el diagrama
 * @author Usuario
 */
public enum TipoCliente {
    BASE("base"),
    MAYOREO("mayoreo"),
    DISTRIBUIDOR("distribuidor"),
    EMPRENDEDOR("emprendedor"),
    SOCIO_COMERCIAL("socio_comercial");

    private final String valor;

    TipoCliente(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static TipoCliente fromValor(String valor) {
        for (TipoCliente tipo : TipoCliente.values()) {
            if (tipo.valor.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de cliente no válido: " + valor);
    }
}
