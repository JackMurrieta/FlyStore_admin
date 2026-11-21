package com.flystore.modulos.gestionarventa;

import com.flystore.persistencia.entidades.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Entidades en memoria para probar los BO de venta sin base de datos. */
final class DatosVenta {
    private DatosVenta() {}

    /** Asigna el id que normalmente genera PostgreSQL. */
    static <T> T conId(T entidad, long id) {
        try {
            Class<?> tipo = entidad.getClass();
            while (tipo != null) {
                try {
                    Field campo = tipo.getDeclaredField("id");
                    campo.setAccessible(true);
                    campo.set(entidad, id);
                    return entidad;
                } catch (NoSuchFieldException e) {
                    tipo = tipo.getSuperclass();
                }
            }
            throw new IllegalArgumentException("Sin campo id: " + entidad.getClass());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static PrecioProducto precio(TipoPrecio tipo, int minPiezas, String precio) {
        var p = new PrecioProducto();
        p.setTipoPrecio(tipo);
        p.setMinPiezas(minPiezas);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }

    /** Gorra con BASE $150 (1 pza), MAYOREO $130 (6 pzas) y DISTRIBUIDOR $110 (12 pzas). */
    static Producto gorra(long id) {
        var producto = conId(new Producto(), id);
        producto.setNombre("Gorra 9FORTY");
        producto.setEstadoProducto(EstadoProducto.PUBLICADO);
        var marca = new Marca();
        marca.setId(1L);
        marca.setNombre("New Era");
        producto.setMarca(marca);
        producto.setPrecios(new ArrayList<>(List.of(
                precio(TipoPrecio.MAYOREO, 6, "130.00"),
                precio(TipoPrecio.BASE, 1, "150.00"),
                precio(TipoPrecio.DISTRIBUIDOR, 12, "110.00"))));
        producto.setImagenes(new ArrayList<>());
        producto.setVariantes(new ArrayList<>());
        return producto;
    }

    static VarianteProducto variante(long id, Producto producto, String color, int stock) {
        var variante = conId(new VarianteProducto(), id);
        variante.setProducto(producto);
        var c = conId(new Color(), id);
        c.setNombreColor(color);
        variante.setColor(c);
        variante.setStock(stock);
        variante.setSku("SKU-" + id);
        producto.getVariantes().add(variante);
        return variante;
    }

    static PedidoVenta carrito(long id, Cliente cliente) {
        var pedido = conId(new PedidoVenta(), id);
        pedido.setCliente(cliente);
        pedido.setEstadoVenta(EstadoVenta.PENDIENTE);
        pedido.setDetalles(new ArrayList<>());
        return pedido;
    }

    static Cliente cliente(long id) {
        var cliente = conId(new Cliente(), id);
        cliente.setNombre("Ana");
        cliente.setApellidos("López");
        cliente.setDatosEntrega(new ArrayList<>());
        return cliente;
    }

    static CodigoDescuento codigo(long id, String texto, String monto) {
        var codigo = conId(new CodigoDescuento(), id);
        codigo.setCodigo(texto);
        codigo.setMontoFijo(new BigDecimal(monto));
        codigo.setActivo(true);
        codigo.setUsosActuales(0);
        return codigo;
    }
}
