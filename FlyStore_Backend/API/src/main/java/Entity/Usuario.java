package Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad Usuario - Clase base para Admin, Cliente y Empleado
 * Vinculada con auth.users de Supabase mediante auth_user_id
 * Usa herencia SINGLE_TABLE
 * @author Usuario
 */
@Entity
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_usuario", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID del usuario en auth.users de Supabase
     * Este campo vincula el usuario de aplicación con el usuario de autenticación
     * Es único porque un usuario de auth solo puede tener un perfil en la app
     */
    @Column(name = "auth_user_id", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID authUserId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 200)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        if (activo == null) {
            activo = true;
        }
    }

    /**
     * Marca el usuario como inactivo (soft delete)
     * No elimina físicamente el registro para preservar datos históricos
     */
    public void desactivar() {
        this.activo = false;
        this.fechaEliminacion = LocalDateTime.now();
    }

    /**
     * Reactiva un usuario previamente desactivado
     */
    public void reactivar() {
        this.activo = true;
        this.fechaEliminacion = null;
    }
}
