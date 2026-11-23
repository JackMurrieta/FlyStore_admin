package com.flystore.persistencia.entidades;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

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

    /**
     * CU-04 · Vencimiento propio del pedido. Solo se llena al rechazar un comprobante (42 h nuevas);
     * si es null, el plazo corre desde fechaHoraPedido.
     */
    @Column(name = "fecha_limite_pago")
    private OffsetDateTime fechaLimitePago;

    /** CU-04 · Datos del envío, capturados al marcar el pedido como ENVIADO. */
    @Column
    private String paqueteria;

    @Column
    private String guia;

    @Column(name = "entrega_local")
    private Boolean entregaLocal;

    @OneToMany(mappedBy = "pedidoVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles;

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public DatosEntrega getDatosEntrega() {
        return datosEntrega;
    }

    public void setDatosEntrega(DatosEntrega datosEntrega) {
        this.datosEntrega = datosEntrega;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public EstadoVenta getEstadoVenta() {
        return estadoVenta;
    }

    public void setEstadoVenta(EstadoVenta estadoVenta) {
        this.estadoVenta = estadoVenta;
    }

    public OffsetDateTime getFechaHoraPedido() {
        return fechaHoraPedido;
    }

    public void setFechaHoraPedido(OffsetDateTime fechaHoraPedido) {
        this.fechaHoraPedido = fechaHoraPedido;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getComprobantePedidoUrl() {
        return comprobantePedidoUrl;
    }

    public void setComprobantePedidoUrl(String comprobantePedidoUrl) {
        this.comprobantePedidoUrl = comprobantePedidoUrl;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }

    public OffsetDateTime getFechaLimitePago() {
        return fechaLimitePago;
    }

    public void setFechaLimitePago(OffsetDateTime fechaLimitePago) {
        this.fechaLimitePago = fechaLimitePago;
    }

    public String getPaqueteria() {
        return paqueteria;
    }

    public void setPaqueteria(String paqueteria) {
        this.paqueteria = paqueteria;
    }

    public String getGuia() {
        return guia;
    }

    public void setGuia(String guia) {
        this.guia = guia;
    }

    public Boolean getEntregaLocal() {
        return entregaLocal;
    }

    public void setEntregaLocal(Boolean entregaLocal) {
        this.entregaLocal = entregaLocal;
    }
}
