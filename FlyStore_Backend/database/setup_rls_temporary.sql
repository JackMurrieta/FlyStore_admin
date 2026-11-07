-- ============================================
-- Row Level Security (RLS) - Configuración Temporal
-- ============================================
-- IMPORTANTE: Estas son políticas PERMISIVAS para desbloquear desarrollo
-- Debes reemplazarlas con políticas apropiadas antes de producción
-- ============================================

-- Paso 1: Habilitar RLS en todas las tablas
-- ============================================

ALTER TABLE usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin ENABLE ROW LEVEL SECURITY;
ALTER TABLE cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE empleado ENABLE ROW LEVEL SECURITY;
ALTER TABLE producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE pedido_venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE categoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE marca ENABLE ROW LEVEL SECURITY;
ALTER TABLE color ENABLE ROW LEVEL SECURITY;
ALTER TABLE talla ENABLE ROW LEVEL SECURITY;
ALTER TABLE tipo_talla ENABLE ROW LEVEL SECURITY;
ALTER TABLE subcategoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE variante_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE imagen_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE precio_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_pedido ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_pedido_variante_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE codigo_descuento ENABLE ROW LEVEL SECURITY;
ALTER TABLE producto_codigo_descuento ENABLE ROW LEVEL SECURITY;
ALTER TABLE detalle_pedido_codigo_descuento ENABLE ROW LEVEL SECURITY;
ALTER TABLE pago ENABLE ROW LEVEL SECURITY;
ALTER TABLE pago_efectivo ENABLE ROW LEVEL SECURITY;
ALTER TABLE pago_transferencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE datos_entrega ENABLE ROW LEVEL SECURITY;
ALTER TABLE dato_bancario ENABLE ROW LEVEL SECURITY;

-- Paso 2: Crear políticas permisivas temporales
-- ============================================
-- ADVERTENCIA: Estas políticas permiten acceso completo a usuarios autenticados
-- Son SOLO para desarrollo. NO usar en producción.

DO $$
DECLARE
    tbl text;
    tables text[] := ARRAY[
        'usuario', 'admin', 'cliente', 'empleado', 'producto', 'pedido_venta',
        'categoria', 'marca', 'color', 'talla', 'tipo_talla', 'subcategoria',
        'variante_producto', 'imagen_producto', 'precio_producto', 'detalle_pedido',
        'detalle_pedido_variante_producto', 'codigo_descuento', 'producto_codigo_descuento',
        'detalle_pedido_codigo_descuento', 'pago', 'pago_efectivo', 'pago_transferencia',
        'datos_entrega', 'dato_bancario'
    ];
BEGIN
    FOREACH tbl IN ARRAY tables
    LOOP
        -- Eliminar política si ya existe
        EXECUTE format('
            DROP POLICY IF EXISTS "Temporary: Allow all for authenticated users" ON %I
        ', tbl);

        -- Crear política permisiva para usuarios autenticados
        EXECUTE format('
            CREATE POLICY "Temporary: Allow all for authenticated users" ON %I
            FOR ALL
            TO authenticated
            USING (true)
            WITH CHECK (true)
        ', tbl);

        RAISE NOTICE 'Política creada para tabla: %', tbl;
    END LOOP;
END $$;

-- ============================================
-- Verificación
-- ============================================

-- Ver todas las políticas creadas
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd
FROM pg_policies
WHERE policyname LIKE 'Temporary:%'
ORDER BY tablename;

-- Ver qué tablas tienen RLS habilitado
SELECT
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
AND rowsecurity = true
ORDER BY tablename;
