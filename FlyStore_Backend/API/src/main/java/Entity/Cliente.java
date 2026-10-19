package Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Cliente - Usuario cliente con tipo de cliente
 * @author Usuario
 */
@Entity
@DiscriminatorValue("cliente")
@Data
@EqualsAndHashCode(callSuper = true)
public class Cliente extends Usuario {

    @Column(length = 255)
    private String direccion;

    @Column(name = "tipo_cliente", nullable = false)
    private TipoCliente tipoCliente = TipoCliente.BASE;

    @OneToMany(mappedBy = "cliente")
    private List<PedidoVenta> pedidos = new ArrayList<>();
}
