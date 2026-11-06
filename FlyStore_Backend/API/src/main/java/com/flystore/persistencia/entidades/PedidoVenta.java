package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad PedidoVenta - Pedido realizado por un cliente
 */
@Entity
@Table(name = "pedido_venta")
public class PedidoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datos_entrega_id")
    private DatosEntrega datosEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Column(name = "costo_envio")
    private BigDecimal costoEnvio;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_venta")
    private EstadoVenta estadoVenta;

    @Column(name = "fecha_hora_pedido")
    private OffsetDateTime fechaHoraPedido;

    @Column(unique = true)
    private String folio;

    @Column(name = "comprobante_pedido_url")
    private String comprobantePedidoUrl;
}
