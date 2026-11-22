-- FLYSTORE 004: hora de llegada del comprobante de transferencia (orden FIFO de revisión de pagos).
-- Para bases que ya existen. Una base nueva ya lo recibe con 000 (columna generada desde la entidad).
-- Revisar y ejecutar manualmente en el SQL Editor de Supabase. Se puede ejecutar más de una vez.
-- No borra datos. Los comprobantes anteriores toman la hora de registro de su pedido.
BEGIN;
SET LOCAL search_path TO public;

ALTER TABLE pago_transferencia ADD COLUMN IF NOT EXISTS fecha_hora_comprobante timestamp(6) with time zone;

UPDATE pago_transferencia pt
SET fecha_hora_comprobante = pv.fecha_hora_pedido
FROM pedido_venta pv
WHERE pv.pago_id = pt.id
  AND pt.comprobante_pago_url IS NOT NULL
  AND pt.fecha_hora_comprobante IS NULL;

COMMIT;

-- Comprobación: debe aparecer la columna fecha_hora_comprobante.
SELECT column_name, data_type FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'pago_transferencia' ORDER BY ordinal_position;
