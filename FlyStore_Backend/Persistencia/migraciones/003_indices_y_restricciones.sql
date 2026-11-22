-- FLYSTORE 003: índices de llaves foráneas, unicidad y validaciones de integridad.
-- Para bases que ya existen (creadas antes de este cambio). Una base nueva ya lo recibe con 000.
-- Revisar y ejecutar manualmente en el SQL Editor de Supabase. No se ejecuta automáticamente.
-- Se puede ejecutar más de una vez: cada objeto se crea solo si no existe.
-- No borra ni modifica datos. Si algún dato actual viola una regla, la transacción se cancela
-- completa y el mensaje indica la tabla y la restricción; corregir ese dato y volver a ejecutar.
-- Estas reglas no sustituyen la lógica del backend: solo impiden guardar datos imposibles.
BEGIN;
SET LOCAL search_path TO public;

-- 1. Índices en llaves foráneas consultadas por el carrito, los pedidos y el catálogo.
--    PostgreSQL no crea índices automáticamente en la columna que referencia.
CREATE INDEX IF NOT EXISTS idx_pedido_venta_cliente_fecha ON pedido_venta (cliente_id, fecha_hora_pedido DESC);
CREATE INDEX IF NOT EXISTS idx_pedido_venta_datos_entrega ON pedido_venta (datos_entrega_id);
CREATE INDEX IF NOT EXISTS idx_pedido_venta_pago ON pedido_venta (pago_id);
CREATE INDEX IF NOT EXISTS idx_detalle_pedido_pedido_venta ON detalle_pedido (pedido_venta_id);
CREATE INDEX IF NOT EXISTS idx_detalle_pedido_variante_detalle ON detalle_pedido_variante_producto (detalle_pedido_id);
CREATE INDEX IF NOT EXISTS idx_detalle_pedido_variante_variante ON detalle_pedido_variante_producto (variante_producto_id);
CREATE INDEX IF NOT EXISTS idx_detalle_pedido_codigo_detalle ON detalle_pedido_codigo_descuento (detalle_pedido_id);
CREATE INDEX IF NOT EXISTS idx_imagen_producto_producto ON imagen_producto (producto_id, orden);
CREATE INDEX IF NOT EXISTS idx_variante_producto_color ON variante_producto (color_id);
CREATE INDEX IF NOT EXISTS idx_variante_producto_talla ON variante_producto (talla_id);
CREATE INDEX IF NOT EXISTS idx_producto_subcategoria ON producto (subcategoria_id);
CREATE INDEX IF NOT EXISTS idx_producto_marca ON producto (marca_id);
CREATE INDEX IF NOT EXISTS idx_producto_codigo_producto ON producto_codigo_descuento (producto_id);
CREATE INDEX IF NOT EXISTS idx_datos_entrega_cliente ON datos_entrega (cliente_id);
CREATE INDEX IF NOT EXISTS idx_subcategoria_categoria ON subcategoria (categoria_id);
CREATE INDEX IF NOT EXISTS idx_tipo_talla_categoria ON tipo_talla (categoria_id);
CREATE INDEX IF NOT EXISTS idx_talla_tipo_talla ON talla (tipo_talla_id);
CREATE INDEX IF NOT EXISTS idx_dato_bancario_admin ON dato_bancario (admin_id);

-- 2. Unicidad. Los índices únicos que empiezan con producto_id también sirven
--    como índice de esa llave foránea; por eso no se crea otro aparte.
-- Un solo carrito (PedidoVenta PENDIENTE) por cliente. Si dos peticiones intentan crearlo
-- al mismo tiempo, la segunda recibe un error de duplicado y el backend vuelve a consultar.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pedido_venta_pendiente_por_cliente
    ON pedido_venta (cliente_id) WHERE estado_venta = 'PENDIENTE';
-- Una variante por producto, color y talla. NULLS NOT DISTINCT trata dos tallas vacías
-- como iguales, así un producto de talla única no repite color.
CREATE UNIQUE INDEX IF NOT EXISTS uk_variante_producto_producto_color_talla
    ON variante_producto (producto_id, color_id, talla_id) NULLS NOT DISTINCT;
-- Un solo precio por tipo (BASE, MAYOREO...) en cada producto.
CREATE UNIQUE INDEX IF NOT EXISTS uk_precio_producto_producto_tipo
    ON precio_producto (producto_id, tipo_precio);

-- 3. Validaciones de valores. NULL se permite; solo se rechazan valores imposibles.
DO $restricciones$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT * FROM (VALUES
            ('variante_producto', 'ck_variante_producto_stock', 'stock >= 0'),
            ('detalle_pedido', 'ck_detalle_pedido_cantidad', 'cantidad > 0'),
            ('detalle_pedido', 'ck_detalle_pedido_precio_unitario', 'precio_unitario >= 0'),
            ('detalle_pedido', 'ck_detalle_pedido_descuento_unitario', 'descuento_unitario >= 0'),
            ('detalle_pedido_variante_producto', 'ck_detalle_pedido_variante_cantidad', 'cantidad_en_combo > 0'),
            ('precio_producto', 'ck_precio_producto_precio', 'precio >= 0'),
            ('precio_producto', 'ck_precio_producto_min_piezas', 'min_piezas >= 1'),
            ('pedido_venta', 'ck_pedido_venta_costo_envio', 'costo_envio >= 0'),
            ('pago', 'ck_pago_monto', 'monto >= 0'),
            ('pago_efectivo', 'ck_pago_efectivo_montos', 'cantidad_recibida >= 0 AND cambio >= 0'),
            ('codigo_descuento', 'ck_codigo_descuento_monto', 'monto_fijo >= 0'),
            ('codigo_descuento', 'ck_codigo_descuento_usos', 'usos_actuales >= 0 AND limite_usos >= 0'),
            ('codigo_descuento', 'ck_codigo_descuento_vigencia', 'fecha_fin >= fecha_inicio')
        ) AS t(tabla, nombre, condicion)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = r.nombre AND conrelid = format('public.%I', r.tabla)::regclass
        ) THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I CHECK (%s)', r.tabla, r.nombre, r.condicion);
        END IF;
    END LOOP;
END
$restricciones$;

COMMIT;

-- Comprobación: deben aparecer los índices idx_/uk_ y las restricciones ck_ creados arriba.
SELECT tablename AS tabla, indexname AS objeto FROM pg_indexes
WHERE schemaname = 'public' AND (indexname LIKE 'idx\_%' OR indexname LIKE 'uk\_%')
UNION ALL
SELECT conrelid::regclass::text, conname FROM pg_constraint
WHERE connamespace = 'public'::regnamespace AND conname LIKE 'ck\_%'
ORDER BY 1, 2;
