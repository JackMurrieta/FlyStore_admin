package com.flystore.persistencia.entidades;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
}
