package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

/**
 * Entidad HistorialPedido - Cada cambio de estado que el personal hace sobre un pedido (CU-04):
 * quién lo hizo, de qué estado a cuál y, si aplica, el motivo.
 */
@Entity
@Table(name = "historial_pedido")
public class HistorialPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_venta_id", nullable = false)
    private PedidoVenta pedidoVenta;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    @Column(nullable = false)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_anterior")
    private EstadoVenta estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoVenta estadoNuevo;

    @Column(length = 500)
    private String motivo;

    @Column(name = "comprobante_rechazado", nullable = false)
    private boolean comprobanteRechazado;

    public Long getId() {
        return id;
    }

    public PedidoVenta getPedidoVenta() {
        return pedidoVenta;
    }

    public void setPedidoVenta(PedidoVenta pedidoVenta) {
        this.pedidoVenta = pedidoVenta;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(OffsetDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public EstadoVenta getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoVenta estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoVenta getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoVenta estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isComprobanteRechazado() {
        return comprobanteRechazado;
    }

    public void setComprobanteRechazado(boolean comprobanteRechazado) {
        this.comprobanteRechazado = comprobanteRechazado;
    }
}
