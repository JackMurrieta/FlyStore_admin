package Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entidad PagoEfectivo - Pago en efectivo
 * @author Usuario
 */
@Entity
@Table(name = "pagos_efectivo")
@DiscriminatorValue("efectivo")
@Data
@EqualsAndHashCode(callSuper = true)
public class PagoEfectivo extends Pago {

    @Column(name = "cantidad_recibida")
    private Double cantidadRecibida;

    @Column
    private Double cambio;
}
