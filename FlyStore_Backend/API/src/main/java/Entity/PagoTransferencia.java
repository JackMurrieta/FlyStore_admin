package Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entidad PagoTransferencia - Pago por transferencia bancaria
 * @author Usuario
 */
@Entity
@Table(name = "pagos_transferencia")
@DiscriminatorValue("transferencia")
@Data
@EqualsAndHashCode(callSuper = true)
public class PagoTransferencia extends Pago {

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Column(name = "numero_referencia", length = 100)
    private String numeroReferencia;
}
