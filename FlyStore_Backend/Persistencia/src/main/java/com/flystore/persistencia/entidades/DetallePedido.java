package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.List;

/**
 * Entidad DetallePedido - Detalle de productos en un pedido
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_venta_id", nullable = false)
    private PedidoVenta pedidoVenta;

    @Column
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_precio")
    private TipoPrecio tipoPrecio;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "descuento_unitario")
    private BigDecimal descuentoUnitario;

    @OneToMany(mappedBy = "detallePedido")
    private List<DetallePedidoVarianteProducto> variantes;

    @OneToMany(mappedBy = "detallePedido")
    private List<DetallePedidoCodigoDescuento> codigosAplicados;

    public Long getId() {
        return id;
    }

    public PedidoVenta getPedidoVenta() {
        return pedidoVenta;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public TipoPrecio getTipoPrecio() {
        return tipoPrecio;
    }

    public void setTipoPrecio(TipoPrecio tipoPrecio) {
        this.tipoPrecio = tipoPrecio;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuentoUnitario() {
        return descuentoUnitario;
    }

    public void setDescuentoUnitario(BigDecimal descuentoUnitario) {
        this.descuentoUnitario = descuentoUnitario;
    }

    public List<DetallePedidoVarianteProducto> getVariantes() {
        return variantes;
    }

    public void setVariantes(List<DetallePedidoVarianteProducto> variantes) {
        this.variantes = variantes;
    }

    public List<DetallePedidoCodigoDescuento> getCodigosAplicados() {
        return codigosAplicados;
    }

    public void setCodigosAplicados(List<DetallePedidoCodigoDescuento> codigosAplicados) {
        this.codigosAplicados = codigosAplicados;
    }
    
    
}
