-- FLYSTORE 005: CU-04 Administrar pedidos.
--   · pedido_venta: vencimiento propio (comprobante rechazado) y datos de envío.
--   · historial_pedido: cada cambio de estado que hace el personal, con usuario y motivo.
-- Para bases que ya existen. Una base nueva ya lo recibe con 000 (generado desde las entidades).
-- Revisar y ejecutar manualmente en el SQL Editor de Supabase. Se puede ejecutar más de una vez.
-- No borra ni modifica datos: solo agrega columnas vacías y una tabla nueva.
BEGIN;
SET LOCAL search_path TO public;

ALTER TABLE pedido_venta ADD COLUMN IF NOT EXISTS fecha_limite_pago timestamp(6) with time zone;
ALTER TABLE pedido_venta ADD COLUMN IF NOT EXISTS paqueteria varchar(255);
ALTER TABLE pedido_venta ADD COLUMN IF NOT EXISTS guia varchar(255);
ALTER TABLE pedido_venta ADD COLUMN IF NOT EXISTS entrega_local boolean;

-- Los estados del historial usan el mismo tipo que pedido_venta.estado_venta, se llame como se llame
-- en esta base (el enum EstadoVenta).
DO $historial$
DECLARE
    tipo_estado text;
BEGIN
    SELECT format_type(a.atttypid, a.atttypmod) INTO tipo_estado
    FROM pg_attribute a
    WHERE a.attrelid = 'public.pedido_venta'::regclass AND a.attname = 'estado_venta' AND NOT a.attisdropped;
    IF tipo_estado IS NULL THEN
        RAISE EXCEPTION 'No existe pedido_venta.estado_venta; revisa que la base tenga el esquema de FlyStore.';
    END IF;
    EXECUTE format($tabla$
        CREATE TABLE IF NOT EXISTS public.historial_pedido (
            id bigserial NOT NULL,
            pedido_venta_id bigint NOT NULL REFERENCES public.pedido_venta,
            fecha_hora timestamp(6) with time zone NOT NULL,
            usuario varchar(255) NOT NULL,
            estado_anterior %s,
            estado_nuevo %s NOT NULL,
            motivo varchar(500),
            comprobante_rechazado boolean NOT NULL,
            PRIMARY KEY (id)
        )$tabla$, tipo_estado, tipo_estado);
END
$historial$;

CREATE INDEX IF NOT EXISTS idx_historial_pedido_pedido_venta ON historial_pedido (pedido_venta_id, fecha_hora DESC);

-- Igual que el resto de las tablas: sin políticas públicas; solo entra el backend.
ALTER TABLE public.historial_pedido ENABLE ROW LEVEL SECURITY;

COMMIT;

-- Comprobación: deben aparecer las cuatro columnas nuevas de pedido_venta y las ocho de historial_pedido.
SELECT table_name, column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND ((table_name = 'pedido_venta' AND column_name IN ('fecha_limite_pago', 'paqueteria', 'guia', 'entrega_local'))
       OR table_name = 'historial_pedido')
ORDER BY table_name, ordinal_position;
