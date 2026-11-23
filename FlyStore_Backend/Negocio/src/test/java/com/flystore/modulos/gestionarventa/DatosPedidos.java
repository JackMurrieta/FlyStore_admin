package com.flystore.modulos.gestionarventa;

import com.flystore.persistencia.entidades.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Entidades en memoria para probar CU-04 Administrar pedidos sin base de datos. */
final class DatosPedidos {
    private DatosPedidos() {}

    static Cliente cliente(long id, String nombre, String apellidos, String telefono) {
        var cliente = DatosVenta.conId(new Cliente(), id);
        cliente.setNombre(nombre);
        cliente.setApellidos(apellidos);
        cliente.setTelefono(telefono);
        return cliente;
    }

    static VarianteProducto variante(long id, String producto, String color, String talla, int stock) {
        var p = DatosVenta.conId(new Producto(), id);
        p.setNombre(producto);
        p.setEstadoProducto(EstadoProducto.PUBLICADO);
        var c = DatosVenta.conId(new Color(), id);
        c.setNombreColor(color);
        var t = DatosVenta.conId(new Talla(), id);
        t.setEtiqueta(talla);
        var variante = DatosVenta.conId(new VarianteProducto(), id);
        variante.setProducto(p);
        variante.setColor(c);
        variante.setTalla(t);
        variante.setStock(stock);
        return variante;
    }

    /** Pedido registrado con una sola línea y datos de entrega; el pago lo pone cada prueba. */
    static PedidoVenta pedido(long id, Cliente cliente, VarianteProducto variante, int piezas, String precio,
                              EstadoVenta estado, OffsetDateTime registrado) {
        var pedido = DatosVenta.conId(new PedidoVenta(), id);
        pedido.setFolio(String.format("FLY-%06d", id));
        pedido.setCliente(cliente);
        pedido.setEstadoVenta(estado);
        pedido.setFechaHoraPedido(registrado);

        var entrega = new DatosEntrega();
        entrega.setNombreCompleto("Ana Torres");
        entrega.setTelefono("6449876543");
        entrega.setDireccion("Calle Uno 123");
        entrega.setCiudad("Ciudad Obregón");
        entrega.setEstado("Sonora");
        pedido.setDatosEntrega(entrega);

        var linea = DatosVenta.conId(new DetallePedido(), id * 10);
        linea.setPedidoVenta(pedido);
        linea.setCantidad(piezas);
        linea.setPrecioUnitario(new BigDecimal(precio));
        var enLinea = new DetallePedidoVarianteProducto();
        enLinea.setDetallePedido(linea);
        enLinea.setVarianteProducto(variante);
        enLinea.setCantidadEnCombo(1);
        linea.setVariantes(new ArrayList<>(List.of(enLinea)));
        linea.setCodigosAplicados(new ArrayList<>());
        pedido.setDetalles(new ArrayList<>(List.of(linea)));
        return pedido;
    }
}
