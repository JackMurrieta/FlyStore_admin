package Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Entidad Empleado - Usuario empleado con fecha de contratación
 * @author Usuario
 */
@Entity
@DiscriminatorValue("empleado")
@Data
@EqualsAndHashCode(callSuper = true)
public class Empleado extends Usuario {

    @Column(name = "fecha_contratacion")
    private LocalDateTime fechaContratacion;
}
