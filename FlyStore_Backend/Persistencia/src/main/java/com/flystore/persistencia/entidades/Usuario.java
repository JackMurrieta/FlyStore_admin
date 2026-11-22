package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad Usuario - Clase base para Admin, Cliente y Empleado
 * Usa herencia JOINED
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_id", nullable = false, unique = true)
    private UUID authId;

    @Column
    private String nombre;

    @Column
    private String apellidos;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column
    private String telefono;

    @Column
    private Boolean activo;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    public Long getId() {
        return id;
    }

    public UUID getAuthId() {
        return authId;
    }

    public void setAuthId(UUID authId) {
        this.authId = authId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    
}
