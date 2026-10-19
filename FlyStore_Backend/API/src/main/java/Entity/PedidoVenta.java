package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad PedidoVenta - Pedido realizado por un cliente
 * @author Usuario
 */
@Entity
@Table(name = "pedidos_venta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "costo_envio", nullable = false)
    private Double costoEnvio = 0.0;

    @Convert(converter = EnumConvert.EstadoVentaConverter.class)
    @Column(name = "estado_venta", nullable = false, columnDefinition = "estado_venta")
    private EstadoVenta estadoVenta = EstadoVenta.PENDIENTE;

    @OneToMany(mappedBy = "pedidoVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "pedidoVenta", cascade = CascadeType.ALL)
    private Pago pago;
}
