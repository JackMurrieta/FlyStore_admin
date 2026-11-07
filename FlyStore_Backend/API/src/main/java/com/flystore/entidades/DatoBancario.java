package com.flystore.entidades;

import jakarta.persistence.*;

/**
 * Entidad DatoBancario - Datos bancarios de administradores
 */
@Entity
@Table(name = "dato_bancario")
public class DatoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "nombre_banco")
    private String nombreBanco;

    @Column
    private String titular;

    @Column(name = "numero_cuenta")
    private String numeroCuenta;

    @Column
    private String clabe;

    @Column
    private Boolean activo;

    public Long getId() {
        return id;
    }

    public Admin getAdmin() {
        return admin;
    }

    public String getNombreBanco() {
        return nombreBanco;
    }

    public void setNombreBanco(String nombreBanco) {
        this.nombreBanco = nombreBanco;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getClabe() {
        return clabe;
    }

    public void setClabe(String clabe) {
        this.clabe = clabe;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    
}
