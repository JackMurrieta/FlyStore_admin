package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import java.util.List;

/**
 * Entidad DatosEntrega - Direcciones de entrega de un cliente
 */
@Entity
@Table(name = "datos_entrega")
public class DatosEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Column
    private String telefono;

    @Column
    private String ciudad;

    @Column
    private String estado;

    @Column
    private String direccion;

    @Column
    private String referencias;

    @Column
    private Boolean activo;

    @OneToMany(mappedBy = "datosEntrega")
    private List<PedidoVenta> pedidos;

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencias() {
        return referencias;
    }

    public void setReferencias(String referencias) {
        this.referencias = referencias;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public List<PedidoVenta> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<PedidoVenta> pedidos) {
        this.pedidos = pedidos;
    }
    
    
}
