package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

/**
 * Entidad Cliente - Usuario cliente con tipo de cliente
 */
@Entity
@Table(name = "cliente")
public class Cliente extends Usuario {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direccion_predeterminada_id")
    private DatosEntrega direccionPredeterminada;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_cliente")
    private TipoCliente tipoCliente;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DatosEntrega> datosEntrega;

    @OneToMany(mappedBy = "cliente")
    private List<PedidoVenta> pedidos;

    public DatosEntrega getDireccionPredeterminada() {
        return direccionPredeterminada;
    }

    public void setDireccionPredeterminada(DatosEntrega direccionPredeterminada) {
        this.direccionPredeterminada = direccionPredeterminada;
    }

    public TipoCliente getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(TipoCliente tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public List<DatosEntrega> getDatosEntrega() {
        return datosEntrega;
    }

    public void setDatosEntrega(List<DatosEntrega> datosEntrega) {
        this.datosEntrega = datosEntrega;
    }

    public List<PedidoVenta> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<PedidoVenta> pedidos) {
        this.pedidos = pedidos;
    }
    
    
}
