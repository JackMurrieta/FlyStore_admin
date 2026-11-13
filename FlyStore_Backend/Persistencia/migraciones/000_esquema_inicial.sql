-- FLYSTORE: instalación inicial, SOLO para una base nueva de Supabase.
-- Incluye el modelo actual y tres productos de prueba; no crea usuarios ni contraseñas.
-- No ejecutar este archivo en una BD con tablas o datos de FlyStore existentes.
-- Todo se ejecuta en una transacción: cualquier error impide confirmar los cambios.
BEGIN;
SET LOCAL search_path TO public;

DO $comprobar$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = ANY (ARRAY['admin', 'categoria', 'cliente', 'codigo_descuento', 'color', 'dato_bancario', 'datos_entrega', 'detalle_pedido', 'detalle_pedido_codigo_descuento', 'detalle_pedido_variante_producto', 'empleado', 'imagen_producto', 'marca', 'pago', 'pago_efectivo', 'pago_transferencia', 'pedido_venta', 'precio_producto', 'producto', 'producto_codigo_descuento', 'subcategoria', 'talla', 'tipo_talla', 'usuario', 'variante_producto'])
    ) THEN
        RAISE EXCEPTION 'Ya existen tablas de FlyStore. No se modificó el esquema; este script es solo para una BD nueva.';
    END IF;
END
$comprobar$;

-- INICIO DDL generado a partir de las entidades JPA.
create type TipoCliente as enum ('BASE','MAYOREO','DISTRIBUIDOR','EMPRENDEDOR','SOCIO_COMERCIAL');

    create cast (varchar as TipoCliente) with inout as implicit;

    create cast (TipoCliente as varchar) with inout as implicit;

    create type TipoPrecio as enum ('BASE','EMPRENDEDOR','MAYOREO','DISTRIBUIDOR','SOCIO_COMERCIAL');

    create cast (varchar as TipoPrecio) with inout as implicit;

    create cast (TipoPrecio as varchar) with inout as implicit;

    create type EstadoPago as enum ('PENDIENTE','EN_PROCESO','COMPLETADO');

    create cast (varchar as EstadoPago) with inout as implicit;

    create cast (EstadoPago as varchar) with inout as implicit;

    create type EstadoVenta as enum ('PENDIENTE','APARTADO_PENDIENTE_PAGO','PENDIENTE_COMPROBANTE','PAGO_EN_REVISION','PAGADO','PREPARACION','ENVIADO','ENTREGADO','CANCELADO','CANCELADO_VENCIMIENTO');

    create cast (varchar as EstadoVenta) with inout as implicit;

    create cast (EstadoVenta as varchar) with inout as implicit;

    create type EstadoProducto as enum ('PUBLICADO','STOCK_BAJO','INACTIVO','BORRADOR');

    create cast (varchar as EstadoProducto) with inout as implicit;

    create cast (EstadoProducto as varchar) with inout as implicit;

    create table admin (
        id bigint not null,
        primary key (id)
    );

    create table categoria (
        id bigserial not null,
        logo_url varchar(255),
        nombre varchar(255),
        primary key (id)
    );

    create table cliente (
        direccion_predeterminada_id bigint,
        id bigint not null,
        tipo_cliente TipoCliente,
        primary key (id)
    );

    create table codigo_descuento (
        activo boolean,
        fecha_creacion date,
        fecha_fin date,
        fecha_inicio date,
        limite_usos integer,
        monto_fijo numeric(38,2),
        usos_actuales integer,
        id bigserial not null,
        codigo varchar(255) unique,
        primary key (id)
    );

    create table color (
        id bigserial not null,
        hex varchar(255),
        nombre_color varchar(255),
        primary key (id)
    );

    create table dato_bancario (
        activo boolean,
        admin_id bigint not null,
        id bigserial not null,
        clabe varchar(255),
        nombre_banco varchar(255),
        numero_cuenta varchar(255),
        titular varchar(255),
        primary key (id)
    );

    create table datos_entrega (
        activo boolean,
        cliente_id bigint not null,
        id bigserial not null,
        ciudad varchar(255),
        direccion varchar(255),
        estado varchar(255),
        nombre_completo varchar(255),
        referencias varchar(255),
        telefono varchar(255),
        primary key (id)
    );

    create table detalle_pedido (
        cantidad integer,
        descuento_unitario numeric(38,2),
        precio_unitario numeric(38,2),
        id bigserial not null,
        pedido_venta_id bigint not null,
        tipo_precio TipoPrecio,
        primary key (id)
    );

    create table detalle_pedido_codigo_descuento (
        codigo_descuento_id bigint not null,
        detalle_pedido_id bigint not null,
        primary key (codigo_descuento_id, detalle_pedido_id)
    );

    create table detalle_pedido_variante_producto (
        cantidad_en_combo integer not null,
        detalle_pedido_id bigint not null,
        id bigserial not null,
        variante_producto_id bigint not null,
        primary key (id)
    );

    create table empleado (
        fecha_contratacion date,
        id bigint not null,
        primary key (id)
    );

    create table imagen_producto (
        orden integer,
        color_id bigint,
        id bigserial not null,
        producto_id bigint not null,
        url_imagen varchar(255),
        primary key (id)
    );

    create table marca (
        activo boolean,
        id bigserial not null,
        logo_url varchar(255),
        nombre varchar(255) not null,
        primary key (id)
    );

    create table pago (
        fecha_pago date,
        monto numeric(38,2),
        id bigserial not null,
        estado_pago EstadoPago,
        primary key (id)
    );

    create table pago_efectivo (
        cambio numeric(38,2),
        cantidad_recibida numeric(38,2),
        id bigint not null,
        primary key (id)
    );

    create table pago_transferencia (
        id bigint not null,
        comprobante_pago_url varchar(255),
        numero_referencia varchar(255),
        primary key (id)
    );

    create table pedido_venta (
        costo_envio numeric(38,2),
        cliente_id bigint not null,
        datos_entrega_id bigint,
        fecha_hora_pedido timestamp(6) with time zone,
        id bigserial not null,
        pago_id bigint,
        comprobante_pedido_url varchar(255),
        folio varchar(255) unique,
        estado_venta EstadoVenta,
        primary key (id)
    );

    create table precio_producto (
        min_piezas integer,
        precio numeric(38,2),
        id bigserial not null,
        producto_id bigint not null,
        tipo_precio TipoPrecio,
        primary key (id)
    );

    create table producto (
        id bigserial not null,
        marca_id bigint not null,
        subcategoria_id bigint not null,
        clave varchar(255) not null unique,
        descripcion TEXT,
        nombre varchar(255),
        estado_producto EstadoProducto,
        primary key (id)
    );

    create table producto_codigo_descuento (
        codigo_descuento_id bigint not null,
        producto_id bigint not null,
        primary key (codigo_descuento_id, producto_id)
    );

    create table subcategoria (
        categoria_id bigint not null,
        id bigserial not null,
        descripcion TEXT,
        nombre varchar(255),
        primary key (id)
    );

    create table talla (
        id bigserial not null,
        tipo_talla_id bigint not null,
        etiqueta varchar(255),
        primary key (id)
    );

    create table tipo_talla (
        categoria_id bigint not null,
        id bigserial not null,
        nombre varchar(255),
        unidad_medida varchar(255),
        primary key (id)
    );

    create table usuario (
        activo boolean,
        fecha_registro date,
        id bigserial not null,
        auth_id uuid not null unique,
        apellidos varchar(255),
        email varchar(255) unique,
        nombre varchar(255),
        password varchar(255),
        telefono varchar(255),
        primary key (id)
    );

    create table variante_producto (
        stock integer,
        color_id bigint not null,
        id bigserial not null,
        producto_id bigint not null,
        talla_id bigint,
        sku varchar(255) unique,
        primary key (id)
    );

    alter table if exists admin 
       add constraint FKiq5uw34yv94wnpdofq0tc4f21 
       foreign key (id) 
       references usuario;

    alter table if exists cliente 
       add constraint FK8nq5mtd2d1khi0r14r7b031y2 
       foreign key (direccion_predeterminada_id) 
       references datos_entrega;

    alter table if exists cliente 
       add constraint FKsitxst8o302fspskxfjatuyrl 
       foreign key (id) 
       references usuario;

    alter table if exists dato_bancario 
       add constraint FKtfgru340vwku67po76i1rsbnx 
       foreign key (admin_id) 
       references admin;

    alter table if exists datos_entrega 
       add constraint FKnwugwdse9ulgm9b3xp7migrbd 
       foreign key (cliente_id) 
       references cliente;

    alter table if exists detalle_pedido 
       add constraint FK7syobevtji2ii9rrk8hbnpe65 
       foreign key (pedido_venta_id) 
       references pedido_venta;

    alter table if exists detalle_pedido_codigo_descuento 
       add constraint FKpbcpuain4by0f68svkcvfxnwp 
       foreign key (codigo_descuento_id) 
       references codigo_descuento;

    alter table if exists detalle_pedido_codigo_descuento 
       add constraint FKt15c0dqa3k0whmxj3e46mcxbv 
       foreign key (detalle_pedido_id) 
       references detalle_pedido;

    alter table if exists detalle_pedido_variante_producto 
       add constraint FKcaqk5mx1rgjyaybl5nx0elro6 
       foreign key (detalle_pedido_id) 
       references detalle_pedido;

    alter table if exists detalle_pedido_variante_producto 
       add constraint FKdj7g47il03pgcswfl4fig5b1i 
       foreign key (variante_producto_id) 
       references variante_producto;

    alter table if exists empleado 
       add constraint FK21ddfrduanruae8nw4y1gbfw 
       foreign key (id) 
       references usuario;

    alter table if exists imagen_producto 
       add constraint FKr9tk0972uq92e5ctwjrkpdttc 
       foreign key (color_id) 
       references color;

    alter table if exists imagen_producto 
       add constraint FK9k9l0b54kcvxup1v39wknhwq1 
       foreign key (producto_id) 
       references producto;

    alter table if exists pago_efectivo 
       add constraint FK9d6e5etq9fpu6fyaa5tby1ax 
       foreign key (id) 
       references pago;

    alter table if exists pago_transferencia 
       add constraint FK6h3wx4qpjp420k4wfiigok9hg 
       foreign key (id) 
       references pago;

    alter table if exists pedido_venta 
       add constraint FK7ams0layckptj1abpgewmcbdp 
       foreign key (cliente_id) 
       references cliente;

    alter table if exists pedido_venta 
       add constraint FKicbq9omn7iwd395cunu2lhy5q 
       foreign key (datos_entrega_id) 
       references datos_entrega;

    alter table if exists pedido_venta 
       add constraint FKe1t3ldop4a4q8k3dxentjtwcp 
       foreign key (pago_id) 
       references pago;

    alter table if exists precio_producto 
       add constraint FKnwda5v6ck4dr9ia65lsv9hokm 
       foreign key (producto_id) 
       references producto;

    alter table if exists producto 
       add constraint FK868tnrt85f21kgcvt9bftgr8r 
       foreign key (marca_id) 
       references marca;

    alter table if exists producto 
       add constraint FKa571qppf005vbgabojxpwgjwe 
       foreign key (subcategoria_id) 
       references subcategoria;

    alter table if exists producto_codigo_descuento 
       add constraint FK98rh6ujqsvi8e0k2ckba88gm 
       foreign key (codigo_descuento_id) 
       references codigo_descuento;

    alter table if exists producto_codigo_descuento 
       add constraint FKntj6j46o3su5gk8tirbd6o5m3 
       foreign key (producto_id) 
       references producto;

    alter table if exists subcategoria 
       add constraint FKmh7cb7m4a5vvvhg5js699qnxk 
       foreign key (categoria_id) 
       references categoria;

    alter table if exists talla 
       add constraint FK4138g66422bu813859vinx8ll 
       foreign key (tipo_talla_id) 
       references tipo_talla;

    alter table if exists tipo_talla 
       add constraint FKdji09re5o83yqrl5e4f9qnadh 
       foreign key (categoria_id) 
       references categoria;

    alter table if exists variante_producto 
       add constraint FKlbg5mu4wnpitkx0y8l66yx3qt 
       foreign key (color_id) 
       references color;

    alter table if exists variante_producto 
       add constraint FKprkb5bek95r4mnsk44osjm7a2 
       foreign key (producto_id) 
       references producto;

    alter table if exists variante_producto 
       add constraint FKntatjdbcblejtlahtgp1mx7nv 
       foreign key (talla_id) 
       references talla;

-- FIN DDL generado.

-- Unicidad de la clave también entre mayúsculas/minúsculas y espacios exteriores.
CREATE UNIQUE INDEX uk_producto_clave_normalizada ON producto (lower(btrim(clave)));

-- Las consultas de la aplicación pasan por el backend, que valida sesión y roles.
-- RLS sin políticas evita exponer estas tablas mediante las claves públicas de Supabase.
ALTER TABLE public.admin ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.codigo_descuento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.color ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dato_bancario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.datos_entrega ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_pedido ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_pedido_codigo_descuento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_pedido_variante_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.empleado ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.imagen_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.marca ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pago ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pago_efectivo ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pago_transferencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pedido_venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.precio_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.producto_codigo_descuento ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subcategoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.talla ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tipo_talla ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.variante_producto ENABLE ROW LEVEL SECURITY;

-- Datos identificados explícitamente como prueba. Los IDs los genera PostgreSQL.
DO $datos$
DECLARE
    categoria_id bigint;
    subcategoria_id bigint;
    marca_id bigint;
    tipo_id bigint;
    talla_id bigint;
    rojo_id bigint;
    negro_id bigint;
    producto_id bigint;
BEGIN
    INSERT INTO categoria(nombre) VALUES ('Gorras de prueba') RETURNING id INTO categoria_id;
    INSERT INTO subcategoria(nombre, descripcion, categoria_id)
        VALUES ('Clásicas', 'Subcategoría para probar filtros', categoria_id) RETURNING id INTO subcategoria_id;
    INSERT INTO marca(nombre, activo) VALUES ('FLY Prueba', true) RETURNING id INTO marca_id;
    INSERT INTO tipo_talla(nombre, unidad_medida, categoria_id)
        VALUES ('Talla de gorra', 'Etiqueta', categoria_id) RETURNING id INTO tipo_id;
    INSERT INTO talla(etiqueta, tipo_talla_id) VALUES ('M', tipo_id) RETURNING id INTO talla_id;
    INSERT INTO talla(etiqueta, tipo_talla_id) VALUES ('L', tipo_id);
    INSERT INTO color(nombre_color, hex) VALUES ('Rojo', '#D62828') RETURNING id INTO rojo_id;
    INSERT INTO color(nombre_color, hex) VALUES ('Negro', '#111111') RETURNING id INTO negro_id;

    INSERT INTO producto(clave, nombre, descripcion, estado_producto, subcategoria_id, marca_id)
        VALUES ('DEMO-001', 'Gorra roja de prueba', 'Producto publicado para probar la consulta',
                'PUBLICADO', subcategoria_id, marca_id) RETURNING id INTO producto_id;
    INSERT INTO variante_producto(producto_id, color_id, talla_id, sku, stock)
        VALUES (producto_id, rojo_id, talla_id, 'DEMO-001-M-ROJO', 10);
    INSERT INTO precio_producto(producto_id, tipo_precio, min_piezas, precio)
        VALUES (producto_id, 'BASE', 1, 150.00), (producto_id, 'MAYOREO', 6, 120.00);

    INSERT INTO producto(clave, nombre, descripcion, estado_producto, subcategoria_id, marca_id)
        VALUES ('DEMO-002', 'Gorra negra en borrador', 'Producto en borrador para probar el filtro de estado',
                'BORRADOR', subcategoria_id, marca_id) RETURNING id INTO producto_id;
    INSERT INTO variante_producto(producto_id, color_id, talla_id, sku, stock)
        VALUES (producto_id, negro_id, talla_id, 'DEMO-002-M-NEGRO', 5);
    INSERT INTO precio_producto(producto_id, tipo_precio, min_piezas, precio)
        VALUES (producto_id, 'BASE', 1, 180.00);

    INSERT INTO producto(clave, nombre, descripcion, estado_producto, subcategoria_id, marca_id)
        VALUES ('DEMO-003', 'Gorra inactiva de prueba', 'Producto inactivo para probar el filtro de estado',
                'INACTIVO', subcategoria_id, marca_id) RETURNING id INTO producto_id;
    INSERT INTO variante_producto(producto_id, color_id, talla_id, sku, stock)
        VALUES (producto_id, negro_id, talla_id, 'DEMO-003-M-NEGRO', 0);
    INSERT INTO precio_producto(producto_id, tipo_precio, min_piezas, precio)
        VALUES (producto_id, 'BASE', 1, 100.00);
END
$datos$;

COMMIT;

-- Deben aparecer tres productos: PUBLICADO, BORRADOR e INACTIVO.
SELECT id, clave, nombre, estado_producto FROM public.producto ORDER BY id;
