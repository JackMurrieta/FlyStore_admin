package com.flystore.entidades;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Entidad CodigoDescuento - Codigos de descuento aplicables a productos
 */
@Entity
@Table(name = "codigo_descuento")
public class CodigoDescuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigo;

    @Column(name = "monto_fijo")
    private BigDecimal montoFijo;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "limite_usos")
    private Integer limiteUsos;

    @Column(name = "usos_actuales")
    private Integer usosActuales;

    @Column
    private Boolean activo;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @OneToMany(mappedBy = "codigoDescuento")
    private List<ProductoCodigoDescuento> productosRelacionados;

    @OneToMany(mappedBy = "codigoDescuento")
    private List<DetallePedidoCodigoDescuento> detallesAplicados;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public BigDecimal getMontoFijo() {
        return montoFijo;
    }

    public void setMontoFijo(BigDecimal montoFijo) {
        this.montoFijo = montoFijo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getLimiteUsos() {
        return limiteUsos;
    }

    public void setLimiteUsos(Integer limiteUsos) {
        this.limiteUsos = limiteUsos;
    }

    public Integer getUsosActuales() {
        return usosActuales;
    }

    public void setUsosActuales(Integer usosActuales) {
        this.usosActuales = usosActuales;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<ProductoCodigoDescuento> getProductosRelacionados() {
        return productosRelacionados;
    }

    public void setProductosRelacionados(List<ProductoCodigoDescuento> productosRelacionados) {
        this.productosRelacionados = productosRelacionados;
    }

    public List<DetallePedidoCodigoDescuento> getDetallesAplicados() {
        return detallesAplicados;
    }

    public void setDetallesAplicados(List<DetallePedidoCodigoDescuento> detallesAplicados) {
        this.detallesAplicados = detallesAplicados;
    }
    
    
}
