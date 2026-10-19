package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Marca - Una marca tiene muchos productos
 * @author Jack Murrieta
 */
@Entity
@Table(name = "marcas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "marca")
    private List<Producto> productos = new ArrayList<>();
}
