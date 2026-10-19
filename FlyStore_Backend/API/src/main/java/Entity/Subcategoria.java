package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad Subcategoria - Muchas subcategorias pertenecen a 1 categoria
 * Relacion: N Subcategorias -> 1 Categoria (Many-to-One)
 * @author Jack Murrieta
 */
@Entity
@Table(name = "subcategorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Relacion Many-to-One con Categoria
     * Muchas subcategorias pertenecen a 1 categoria
     * JoinColumn: especifica la columna FK en la tabla subcategorias
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}
