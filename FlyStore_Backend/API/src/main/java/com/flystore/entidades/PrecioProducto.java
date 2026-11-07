package com.flystore.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;

/**
 * Entidad PrecioProducto - Define diferentes precios para un producto segun tipo
 */
@Entity
@Table(name = "precio_producto")
public class PrecioProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_precio")
    private TipoPrecio tipoPrecio;

    @Column(name = "min_piezas")
    private Integer minPiezas;

    @Column
    private BigDecimal precio;

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public TipoPrecio getTipoPrecio() {
        return tipoPrecio;
    }

    public void setTipoPrecio(TipoPrecio tipoPrecio) {
        this.tipoPrecio = tipoPrecio;
    }

    public Integer getMinPiezas() {
        return minPiezas;
    }

    public void setMinPiezas(Integer minPiezas) {
        this.minPiezas = minPiezas;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
    
    
}
