
package DTOs;
import lombok.Data;
import java.util.UUID;

/**
 * DTO para registro de nuevos clientes
 * @author Usuario
 */
@Data
public class RegistroClienteDTO {
    private UUID authUserId;      // ID del usuario en auth.users de Supabase
    private String nombre;
    private String apellidos;
    private String email;
    private String telefono;
    private String fotoPerfil;
    private String direccion;
}
