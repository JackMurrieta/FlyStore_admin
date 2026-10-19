package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad DetallePedido - Detalle de productos en un pedido
 * Usa snapshot pattern para precios y descuentos (inmutables ante cambios futuros)
 * @author Usuario
 */
@Entity
@Table(name = "detalles_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_venta_id", nullable = false)
    private PedidoVenta pedidoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_producto_id", nullable = false)
    private VarianteProducto varianteProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_precio", nullable = false)
    private TipoPrecio tipoPrecio;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    @Column(name = "descuento_unitario")
    private Double descuentoUnitario = 0.0;

    @ManyToMany
    @JoinTable(
        name = "detalle_pedido_codigo_descuento",
        joinColumns = @JoinColumn(name = "detalle_pedido_id"),
        inverseJoinColumns = @JoinColumn(name = "codigo_descuento_id")
    )
    private List<CodigoDescuento> codigosAplicados = new ArrayList<>();

    public Double getSubtotalSinDescuento() {
        return cantidad * precioUnitario;
    }

    public Double getDescuentoTotal() {
        return cantidad * descuentoUnitario;
    }

    public Double getSubtotal() {
        return getSubtotalSinDescuento() - getDescuentoTotal();
    }
}
