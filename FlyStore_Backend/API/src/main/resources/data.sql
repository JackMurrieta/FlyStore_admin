-- ============================================
-- Script de inicialización automática
-- Se ejecuta después de que Hibernate crea las tablas
-- ============================================

-- Paso 1: Habilitar Row Level Security en todas las tablas
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

-- Paso 2: Crear funciones auxiliares para RLS
-- ============================================

-- Función para obtener el UUID del usuario autenticado desde el JWT
CREATE OR REPLACE FUNCTION auth.uid()
RETURNS uuid AS $$
    SELECT NULLIF(
        current_setting('request.jwt.claims', TRUE)::json->>'sub',
        ''
    )::uuid;
$$ LANGUAGE SQL STABLE;

-- Función para verificar si el usuario actual es admin
CREATE OR REPLACE FUNCTION is_admin()
RETURNS boolean AS $$
    SELECT EXISTS (
        SELECT 1 FROM admin a
        INNER JOIN usuario u ON u.id = a.id
        WHERE u.auth_id = auth.uid()
    );
$$ LANGUAGE SQL STABLE;

-- Función para verificar si el usuario actual es empleado o admin
CREATE OR REPLACE FUNCTION is_staff()
RETURNS boolean AS $$
    SELECT EXISTS (
        SELECT 1 FROM usuario u
        LEFT JOIN admin a ON a.id = u.id
        LEFT JOIN empleado e ON e.id = u.id
        WHERE u.auth_id = auth.uid()
        AND (a.id IS NOT NULL OR e.id IS NOT NULL)
    );
$$ LANGUAGE SQL STABLE;

-- Función para obtener el ID interno del usuario desde auth_id
CREATE OR REPLACE FUNCTION get_user_id()
RETURNS bigint AS $$
    SELECT id FROM usuario WHERE auth_id = auth.uid();
$$ LANGUAGE SQL STABLE;

-- Paso 3: Crear políticas RLS permisivas para DESARROLLO
-- ============================================
-- IMPORTANTE: Estas políticas permiten acceso completo a usuarios autenticados
-- Son SOLO para desarrollo. Reemplázalas con políticas apropiadas en producción
-- ============================================

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
        -- Eliminar política si ya existe (por si se re-ejecuta el script)
        EXECUTE format('
            DROP POLICY IF EXISTS "DEV: Allow all for authenticated users" ON %I
        ', tbl);

        -- Crear política permisiva para usuarios autenticados
        EXECUTE format('
            CREATE POLICY "DEV: Allow all for authenticated users" ON %I
            FOR ALL
            TO authenticated
            USING (true)
            WITH CHECK (true)
        ', tbl);

        RAISE NOTICE 'RLS habilitado y política creada para tabla: %', tbl;
    END LOOP;
END $$;

-- ============================================
-- Datos de prueba (opcional - descomenta si necesitas)
-- ============================================

-- Ejemplo: Insertar categorías de prueba
-- INSERT INTO categoria (nombre, descripcion) VALUES
--     ('Camisetas', 'Camisetas de todo tipo'),
--     ('Pantalones', 'Pantalones y jeans'),
--     ('Zapatos', 'Calzado deportivo y casual');

-- Ejemplo: Insertar marcas de prueba
-- INSERT INTO marca (nombre) VALUES
--     ('Nike'),
--     ('Adidas'),
--     ('Puma');

-- Ejemplo: Insertar colores de prueba
-- INSERT INTO color (nombre, codigo_hex) VALUES
--     ('Negro', '#000000'),
--     ('Blanco', '#FFFFFF'),
--     ('Rojo', '#FF0000'),
--     ('Azul', '#0000FF');

-- ============================================
-- Fin del script
-- ============================================
