package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad PrecioProducto - Define diferentes precios para un producto segun tipo
 * Relacion: N PrecioProducto -> 1 Producto (Many-to-One)
 * @author Usuario
 */
@Entity
@Table(name = "precios_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecioProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_precio", nullable = false)
    private TipoPrecio tipoPrecio;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Boolean activo = true;
}
