-- =====================================================================
-- FlyStore · schema.sql
-- Generado a partir de Diagrama_Dominio.svg (sin cambios al modelo).
-- Base de datos: PostgreSQL (Supabase), esquema public.
--
-- Convenciones de traducción (no cambian el modelo):
--   · Nombres en snake_case (lo que espera Spring Boot / Hibernate por defecto).
--   · Cada entidad lleva id BIGINT IDENTITY (en el diagrama solo Usuario lo muestra).
--   · String -> text, Integer -> integer, Long -> bigint, double -> double precision,
--     boolean/Boolean -> boolean, UUID -> uuid, Date -> date, DateTime -> timestamptz.
--   · Los enums del diagrama son tipos ENUM de Postgres con los mismos valores.
--   · Herencia (Usuario y Pago) como tablas unidas (JPA InheritanceType.JOINED):
--     la tabla hija usa como PK el id de la tabla padre.
--   · Relaciones *..* como tabla intermedia.
--   · Se respetan los nombres tal como están, incluido estado_veneta.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------
create type tipo_usuario as enum ('cliente', 'admin', 'empleado');

create type tipo_cliente as enum ('base', 'mayoreo', 'distribuidor', 'emprendedor', 'socio_comercial');

create type estado_venta as enum (
    'pendiente',
    'apartado_pendiente_pago',
    'pendiente_comprobante',
    'pago_en_revision',
    'pagado',
    'preparacion',
    'enviado',
    'entregado',
    'cancelado',
    'cancelado_vencimiento'
);

create type tipo_precio as enum ('base', 'emprendedor', 'mayoreo', 'distribuidor', 'socio_comercial');

create type estado_pago as enum ('pendiente', 'en_proceso', 'completado');

create type estado_producto as enum ('publicado', 'stock_bajo', 'inactivo', 'borrador');

-- ---------------------------------------------------------------------
-- Usuarios (Usuario <- Cliente, Admin, Empleado)
-- ---------------------------------------------------------------------
create table usuario (
    id              bigint generated always as identity primary key,
    auth_id         uuid not null unique,
    nombre          text,
    apellidos       text,
    email           text unique,
    password        text,
    telefono        text,
    tipo_usuario    tipo_usuario,
    activo          boolean,
    fecha_registro  date
);

create table cliente (
    id                            bigint primary key references usuario (id) on delete cascade,
    direccion_predeterminada_id   bigint,            -- FK a datos_entrega (se agrega más abajo)
    tipo_cliente                  tipo_cliente
);

create table admin (
    id  bigint primary key references usuario (id) on delete cascade
);

create table empleado (
    id                  bigint primary key references usuario (id) on delete cascade,
    fecha_contratacion  date
);

-- Admin 1 — 1..* DatoBancario
create table dato_bancario (
    id              bigint generated always as identity primary key,
    admin_id        bigint not null references admin (id),
    nombre_banco    text,
    titular         text,
    numero_cuenta   text,
    clabe           text,
    activo          boolean
);

-- Cliente 1 — 1..* DatosEntrega
create table datos_entrega (
    id                bigint generated always as identity primary key,
    cliente_id        bigint not null references cliente (id),
    nombre_completo   text,
    telefono          text,
    ciudad            text,
    estado            text,
    direccion         text,
    referencias       text,
    predeterminada    boolean
);

-- Cliente.direccionPredeterminada : DatosEntrega
alter table cliente
    add constraint fk_cliente_direccion_predeterminada
    foreign key (direccion_predeterminada_id) references datos_entrega (id);

-- ---------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------
create table categoria (
    id      bigint generated always as identity primary key,
    nombre  text
);

-- Categoria 1 — 1..* Subcategoria
create table subcategoria (
    id            bigint generated always as identity primary key,
    categoria_id  bigint not null references categoria (id),
    nombre        text,
    descripcion   text
);

-- Categoria 1 — 1..* TipoTalla
create table tipo_talla (
    id             bigint generated always as identity primary key,
    categoria_id   bigint not null references categoria (id),
    nombre         text,   -- Numerica, Alfabetica, Talla unica
    unidad_medida  text    -- cm o in
);

-- TipoTalla 1 — 1..* Talla
create table talla (
    id             bigint generated always as identity primary key,
    tipo_talla_id  bigint not null references tipo_talla (id),
    etiqueta       text
);

create table marca (
    id        bigint generated always as identity primary key,
    nombre    text,
    logo_url  text,
    activo    boolean
);

create table color (
    id            bigint generated always as identity primary key,
    nombre_color  text,
    hex           text
);

-- Subcategoria 1 — 1..* Producto, Marca 1 — 1..* Producto, Producto — EstadoProducto
create table producto (
    id               bigint generated always as identity primary key,
    subcategoria_id  bigint not null references subcategoria (id),
    marca_id         bigint not null references marca (id),
    nombre           text,
    descripcion      text,
    estado_producto  estado_producto
);

-- Producto ◆ VarianteProducto, Color 1 — 1..* VarianteProducto, talla : Talla
create table variante_producto (
    id           bigint generated always as identity primary key,
    producto_id  bigint not null references producto (id) on delete cascade,
    color_id     bigint not null references color (id),
    talla_id     bigint references talla (id),
    sku          text unique,
    stock        integer
);

-- Producto ◆ ImagenProducto, Color 1 — 1..* ImagenProducto
create table imagen_producto (
    id           bigint generated always as identity primary key,
    producto_id  bigint not null references producto (id) on delete cascade,
    color_id     bigint not null references color (id),
    url_imagen   text,
    orden        integer
);

-- Producto — PrecioProducto (PrecioProducto.producto_id)
create table precio_producto (
    id           bigint generated always as identity primary key,
    producto_id  bigint not null references producto (id) on delete cascade,
    tipo_precio  tipo_precio,
    min_piezas   integer,
    precio       double precision
);

create table codigo_descuento (
    id              bigint generated always as identity primary key,
    codigo          text unique,
    monto_fijo      double precision,
    fecha_inicio    date,
    fecha_fin       date,
    limite_usos     integer,
    usos_actuales   integer,
    activo          boolean,
    fecha_creacion  date
);

-- Producto 1..* — 1..* CodigoDescuento
create table producto_codigo_descuento (
    producto_id          bigint not null references producto (id) on delete cascade,
    codigo_descuento_id  bigint not null references codigo_descuento (id) on delete cascade,
    primary key (producto_id, codigo_descuento_id)
);

-- ---------------------------------------------------------------------
-- Pagos (Pago <- PagoEfectivo, PagoTransferencia)
-- ---------------------------------------------------------------------
create table pago (
    id           bigint generated always as identity primary key,
    monto        double precision,
    fecha_pago   date,
    estado_pago  estado_pago
);

create table pago_efectivo (
    id                 bigint primary key references pago (id) on delete cascade,
    cantidad_recibida  double precision,
    cambio             double precision
);

create table pago_transferencia (
    id                     bigint primary key references pago (id) on delete cascade,
    comprobante_pago_url   text,
    numero_referencia      text
);

-- ---------------------------------------------------------------------
-- Ventas
-- ---------------------------------------------------------------------
-- Cliente 1 — 1..* PedidoVenta, PedidoVenta 1 — 1 DatosEntrega, PedidoVenta 1 — 1 Pago
create table pedido_venta (
    id                      bigint generated always as identity primary key,
    cliente_id              bigint not null references cliente (id),
    datos_entrega_id        bigint references datos_entrega (id),   -- se llena al capturar la entrega
    pago_id                 bigint unique references pago (id),      -- se llena al registrar el pago
    costo_envio             double precision,
    estado_veneta           estado_venta,
    fecha_hora_pedido       timestamptz,
    folio                   text unique,
    comprobante_pedido_url  text
);

-- PedidoVenta ◆ DetallePedido, VarianteProducto — DetallePedido
create table detalle_pedido (
    id                    bigint generated always as identity primary key,
    pedido_venta_id       bigint not null references pedido_venta (id) on delete cascade,
    variante_producto_id  bigint not null references variante_producto (id),
    cantidad              integer,
    tipo_precio           tipo_precio,
    precio_unitario       double precision,
    descuento_unitario    double precision
);

-- DetallePedido 0..* — 0..* CodigoDescuento
-- Nota del diagrama: máx. 1 código por detalle; el descuento queda en descuento_unitario.
create table detalle_pedido_codigo_descuento (
    detalle_pedido_id    bigint not null references detalle_pedido (id) on delete cascade,
    codigo_descuento_id  bigint not null references codigo_descuento (id),
    primary key (detalle_pedido_id, codigo_descuento_id)
);

-- ---------------------------------------------------------------------
-- Índices de llaves foráneas
-- ---------------------------------------------------------------------
create index on dato_bancario (admin_id);
create index on datos_entrega (cliente_id);
create index on subcategoria (categoria_id);
create index on tipo_talla (categoria_id);
create index on talla (tipo_talla_id);
create index on producto (subcategoria_id);
create index on producto (marca_id);
create index on variante_producto (producto_id);
create index on variante_producto (color_id);
create index on imagen_producto (producto_id);
create index on precio_producto (producto_id);
create index on pedido_venta (cliente_id);
create index on detalle_pedido (pedido_venta_id);
create index on detalle_pedido (variante_producto_id);

-- ---------------------------------------------------------------------
-- RLS activado sin políticas: la API pública de Supabase (anon / authenticated)
-- no ve ni modifica nada. El backend Spring Boot entra por JDBC con el usuario
-- postgres, que no queda limitado por RLS.
-- ---------------------------------------------------------------------
do $$
declare r record;
begin
    for r in select tablename from pg_tables where schemaname = 'public' loop
        execute format('alter table public.%I enable row level security', r.tablename);
    end loop;
end $$;
