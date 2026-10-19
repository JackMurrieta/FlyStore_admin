package Entity;

/**
 * Enum Talla - Define tallas generales y tallas específicas New Era
 * @author Jack Murrieta
 */
public enum Talla {
    // Tallas generales
    S("s", "S"),
    M("m", "M"),
    L("l", "L"),
    XL("xl", "XL"),
    XXL("xxl", "XXL"),

    // Tallas New Era gorras
    TALLA_6_7_8("6_7_8", "6 7/8"),
    TALLA_7("7", "7"),
    TALLA_7_1_8("7_1_8", "7 1/8"),
    TALLA_7_1_4("7_1_4", "7 1/4"),
    TALLA_7_3_8("7_3_8", "7 3/8"),
    TALLA_7_1_2("7_1_2", "7 1/2"),
    TALLA_7_5_8("7_5_8", "7 5/8"),
    TALLA_7_3_4("7_3_4", "7 3/4"),
    TALLA_7_7_8("7_7_8", "7 7/8"),
    TALLA_8("8", "8");

    private final String valor;
    private final String etiqueta;

    Talla(String valor, String etiqueta) {
        this.valor = valor;
        this.etiqueta = etiqueta;
    }

    public String getValor() {
        return valor;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esNewEra() {
        return this.ordinal() >= TALLA_6_7_8.ordinal();
    }

    public boolean esGeneral() {
        return this.ordinal() < TALLA_6_7_8.ordinal();
    }

    public static Talla fromValor(String valor) {
        for (Talla talla : Talla.values()) {
            if (talla.valor.equalsIgnoreCase(valor)) {
                return talla;
            }
        }
        throw new IllegalArgumentException("Talla no válida: " + valor);
    }
}
