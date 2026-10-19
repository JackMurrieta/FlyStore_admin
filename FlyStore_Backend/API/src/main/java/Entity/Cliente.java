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

    @Convert(converter = EnumConvert.TipoClienteConverter.class)
    @Column(name = "tipo_cliente", nullable = false, columnDefinition = "tipo_cliente")
    private TipoCliente tipoCliente = TipoCliente.BASE;

    @OneToMany(mappedBy = "cliente")
    private List<PedidoVenta> pedidos = new ArrayList<>();
}
