package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad CodigoDescuento - Codigos de descuento aplicables a productos
 * Relacion Many-to-Many con Producto
 * @author Usuario
 */
@Entity
@Table(name = "codigos_descuento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "tipo_descuento", nullable = false, length = 20)
    private String tipoDescuento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    @Column(name = "usos_actuales")
    private Integer usosActuales = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany(mappedBy = "codigosDescuento")
    private List<Producto> productos = new ArrayList<>();

    @ManyToMany(mappedBy = "codigosAplicados")
    private List<DetallePedido> detallesAplicados = new ArrayList<>();
}
