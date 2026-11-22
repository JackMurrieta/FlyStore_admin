package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.util.List;

/**
 * Entidad Admin - Usuario administrador
 */
@Entity
@Table(name = "admin")
public class Admin extends Usuario {

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatoBancario> datosBancarios;

    public List<DatoBancario> getDatosBancarios() {
        return datosBancarios;
    }
    
}
