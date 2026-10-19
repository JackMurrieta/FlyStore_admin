package Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entidad Admin - Usuario administrador
 * @author Usuario
 */
@Entity
@DiscriminatorValue("admin")
@Data
@EqualsAndHashCode(callSuper = true)
public class Admin extends Usuario {
}
