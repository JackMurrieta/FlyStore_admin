package Entity;

/**
 * Enum TipoUsuario - Define los tipos de usuario del sistema
 * @author Usuario
 */
public enum TipoUsuario {
    CLIENTE("cliente"),
    ADMIN("admin"),
    EMPLEADO("empleado");

    private final String valor;

    TipoUsuario(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static TipoUsuario fromValor(String valor) {
        for (TipoUsuario tipo : TipoUsuario.values()) {
            if (tipo.valor.equalsIgnoreCase(valor)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de usuario no válido: " + valor);
    }
}
