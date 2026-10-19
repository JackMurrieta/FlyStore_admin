package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad ImagenProducto - Muchas imagenes pertenecen a un producto
 * Relacion: N ImagenProducto -> 1 Producto (Many-to-One)
 * @author Jack Murrieta
 */
@Entity
@Table(name = "imagenes_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "url_imagen", nullable = false)
    private String urlImagen;

    @Column(nullable = false)
    private Integer orden = 0;
}
