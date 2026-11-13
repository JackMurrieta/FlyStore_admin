-- Revisar y ejecutar manualmente antes de desplegar el catálogo. No se ejecuta automáticamente.
BEGIN;
ALTER TABLE producto ADD COLUMN IF NOT EXISTS clave varchar(255);
-- Identifica los productos anteriores sin eliminar datos ni cambiar sus identificadores.
UPDATE producto SET clave = 'LEGACY-' || id::text WHERE clave IS NULL OR btrim(clave) = '';
ALTER TABLE producto ALTER COLUMN clave SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_producto_clave_normalizada ON producto (lower(btrim(clave)));
ALTER TABLE imagen_producto ALTER COLUMN color_id DROP NOT NULL;
COMMIT;
