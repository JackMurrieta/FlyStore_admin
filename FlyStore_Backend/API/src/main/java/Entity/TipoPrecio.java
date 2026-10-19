package Entity;

/**
 * Enum TipoPrecio - Define los tipos de precio disponibles
 * @author Jack Murrieta
 */
public enum TipoPrecio {
    BASE("base"),
    EMPRENDEDOR("emprendedor"),
    MAYOREO("mayoreo"),
    DISTRIBUIDOR("distribuidor"),
    SOCIO_COMERCIAL("socio_comercial");

    private final String valor;

    TipoPrecio(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static TipoPrecio fromValor(String valor) {
        for (TipoPrecio tipo : TipoPrecio.values()) {
            if (tipo.valor.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de precio no válido: " + valor);
    }
}
