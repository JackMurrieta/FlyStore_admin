package com.flystore.persistencia.entidades;

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
}
