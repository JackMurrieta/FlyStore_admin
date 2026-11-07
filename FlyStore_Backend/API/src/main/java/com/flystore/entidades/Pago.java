package com.flystore.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidad Pago - Clase base para PagoEfectivo y PagoTransferencia
 * Usa herencia JOINED
 */
@Entity
@Table(name = "pago")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private BigDecimal monto;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_pago")
    private EstadoPago estadoPago;

    @OneToMany(mappedBy = "pago")
    private List<PedidoVenta> pedidos;

    public Long getId() {
        return id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    public List<PedidoVenta> getPedidos() {
        return pedidos;
    }

    public void setPedidos(List<PedidoVenta> pedidos) {
        this.pedidos = pedidos;
    }
    
    
}
