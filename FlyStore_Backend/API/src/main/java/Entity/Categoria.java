package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Categoria - Una categoria tiene muchas subcategorias
 * Relacion: 1 Categoria -> N Subcategorias (One-to-Many)
 * @author JHack Murrieta
 */
@Entity
@Table(name = "categorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "logo_url")
    private String logoUrl;

    /**
     * Relacion One-to-Many con Subcategoria
     * mappedBy: indica que la relacion esta mapeada por el campo "categoria" en Subcategoria
     * cascade: cuando se elimina una categoria, se eliminan sus subcategorias
     * orphanRemoval: si se remueve una subcategoria de la lista, se elimina de la BD
     */
    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subcategoria> subcategorias = new ArrayList<>();

}
