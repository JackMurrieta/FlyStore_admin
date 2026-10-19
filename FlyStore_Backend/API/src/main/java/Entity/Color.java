package Entity;

/**
 * Enum Color - Define los colores disponibles con sus códigos hexadecimales
 * @author Jack Murrieta
 */
public enum Color {
    NEGRO("negro", "#000000"),
    BLANCO("blanco", "#FFFFFF"),
    AZUL("azul", "#0000FF"),
    ROJO("rojo", "#FF0000"),
    VERDE("verde", "#00FF00"),
    CAFE("cafe", "#8B4513"),
    VIOLETA("violeta", "#8B00FF"),
    ROSA("rosa", "#FFC0CB"),
    NARANJA("naranja", "#FFA500"),
    AMARILLO("amarillo", "#FFFF00"),
    GRIS("gris", "#808080");

    private final String valor;
    private final String codigoHex;

    Color(String valor, String codigoHex) {
        this.valor = valor;
        this.codigoHex = codigoHex;
    }

    public String getValor() {
        return valor;
    }

    public String getCodigoHex() {
        return codigoHex;
    }

    public static Color fromValor(String valor) {
        for (Color color : Color.values()) {
            if (color.valor.equalsIgnoreCase(valor)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Color no válido: " + valor);
    }
}
