-- ============================================
-- Row Level Security (RLS) - Políticas de Producción
-- ============================================
-- IMPORTANTE: Estas políticas implementan seguridad apropiada para producción
-- Reemplazan las políticas temporales permisivas
-- ============================================

-- Paso 1: Eliminar políticas temporales
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
        EXECUTE format('
            DROP POLICY IF EXISTS "Temporary: Allow all for authenticated users" ON %I
        ', tbl);
    END LOOP;
END $$;

-- ============================================
-- Funciones auxiliares para RLS
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

-- ============================================
-- Políticas para tabla USUARIO
-- ============================================

-- Los usuarios pueden ver su propio perfil
CREATE POLICY "Users can view own profile" ON usuario
    FOR SELECT
    TO authenticated
    USING (auth_id = auth.uid());

-- Los usuarios pueden actualizar su propio perfil
CREATE POLICY "Users can update own profile" ON usuario
    FOR UPDATE
    TO authenticated
    USING (auth_id = auth.uid())
    WITH CHECK (auth_id = auth.uid());

-- Los admins pueden ver todos los usuarios
CREATE POLICY "Admins can view all users" ON usuario
    FOR SELECT
    TO authenticated
    USING (is_admin());

-- Los admins pueden insertar nuevos usuarios
CREATE POLICY "Admins can insert users" ON usuario
    FOR INSERT
    TO authenticated
    WITH CHECK (is_admin());

-- ============================================
-- Políticas para ADMIN, CLIENTE, EMPLEADO
-- ============================================

-- Los usuarios pueden ver sus propios registros extendidos
CREATE POLICY "Users can view own admin record" ON admin
    FOR SELECT
    TO authenticated
    USING (id IN (SELECT id FROM usuario WHERE auth_id = auth.uid()));

CREATE POLICY "Users can view own cliente record" ON cliente
    FOR SELECT
    TO authenticated
    USING (id IN (SELECT id FROM usuario WHERE auth_id = auth.uid()));

CREATE POLICY "Users can view own empleado record" ON empleado
    FOR SELECT
    TO authenticated
    USING (id IN (SELECT id FROM usuario WHERE auth_id = auth.uid()));

-- Los admins pueden gestionar todos los registros
CREATE POLICY "Admins manage all admin records" ON admin
    FOR ALL
    TO authenticated
    USING (is_admin())
    WITH CHECK (is_admin());

CREATE POLICY "Admins manage all cliente records" ON cliente
    FOR ALL
    TO authenticated
    USING (is_admin())
    WITH CHECK (is_admin());

CREATE POLICY "Admins manage all empleado records" ON empleado
    FOR ALL
    TO authenticated
    USING (is_admin())
    WITH CHECK (is_admin());

-- ============================================
-- Políticas para PRODUCTO
-- ============================================

-- Todos pueden ver productos activos
CREATE POLICY "Anyone can view active products" ON producto
    FOR SELECT
    TO authenticated
    USING (estado = 'ACTIVO');

-- Staff puede ver todos los productos (incluyendo inactivos)
CREATE POLICY "Staff can view all products" ON producto
    FOR SELECT
    TO authenticated
    USING (is_staff());

-- Solo staff puede modificar productos
CREATE POLICY "Staff can modify products" ON producto
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para CATEGORÍA, MARCA, etc.
-- ============================================

-- Todos pueden ver datos de catálogo
CREATE POLICY "Anyone can view categorias" ON categoria
    FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Anyone can view marcas" ON marca
    FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Anyone can view colores" ON color
    FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Anyone can view tallas" ON talla
    FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Anyone can view tipo_talla" ON tipo_talla
    FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "Anyone can view subcategorias" ON subcategoria
    FOR SELECT
    TO authenticated
    USING (true);

-- Solo staff puede modificar datos de catálogo
CREATE POLICY "Staff can modify categorias" ON categoria
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can modify marcas" ON marca
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can modify colores" ON color
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can modify tallas" ON talla
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can modify tipo_talla" ON tipo_talla
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can modify subcategorias" ON subcategoria
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para VARIANTE_PRODUCTO, IMAGEN_PRODUCTO, PRECIO_PRODUCTO
-- ============================================

-- Todos pueden ver variantes, imágenes y precios de productos activos
CREATE POLICY "Anyone can view variantes of active products" ON variante_producto
    FOR SELECT
    TO authenticated
    USING (
        producto_id IN (SELECT id FROM producto WHERE estado = 'ACTIVO')
    );

CREATE POLICY "Anyone can view images of active products" ON imagen_producto
    FOR SELECT
    TO authenticated
    USING (
        producto_id IN (SELECT id FROM producto WHERE estado = 'ACTIVO')
    );

CREATE POLICY "Anyone can view prices of active products" ON precio_producto
    FOR SELECT
    TO authenticated
    USING (
        producto_id IN (SELECT id FROM producto WHERE estado = 'ACTIVO')
    );

-- Staff puede gestionar variantes, imágenes y precios
CREATE POLICY "Staff can manage variantes" ON variante_producto
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage images" ON imagen_producto
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage prices" ON precio_producto
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para PEDIDO_VENTA
-- ============================================

-- Los clientes solo pueden ver sus propios pedidos
CREATE POLICY "Clients can view own orders" ON pedido_venta
    FOR SELECT
    TO authenticated
    USING (
        cliente_id IN (
            SELECT id FROM cliente
            WHERE id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
        )
    );

-- Los clientes pueden crear sus propios pedidos
CREATE POLICY "Clients can create own orders" ON pedido_venta
    FOR INSERT
    TO authenticated
    WITH CHECK (
        cliente_id IN (
            SELECT id FROM cliente
            WHERE id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
        )
    );

-- Staff puede ver y gestionar todos los pedidos
CREATE POLICY "Staff can view all orders" ON pedido_venta
    FOR SELECT
    TO authenticated
    USING (is_staff());

CREATE POLICY "Staff can update orders" ON pedido_venta
    FOR UPDATE
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para DETALLE_PEDIDO y relaciones
-- ============================================

-- Los clientes pueden ver detalles de sus propios pedidos
CREATE POLICY "Clients can view own order details" ON detalle_pedido
    FOR SELECT
    TO authenticated
    USING (
        pedido_id IN (
            SELECT id FROM pedido_venta pv
            WHERE pv.cliente_id IN (
                SELECT id FROM cliente c
                WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
            )
        )
    );

CREATE POLICY "Clients can view own order variant details" ON detalle_pedido_variante_producto
    FOR SELECT
    TO authenticated
    USING (
        detalle_pedido_id IN (
            SELECT id FROM detalle_pedido dp
            WHERE dp.pedido_id IN (
                SELECT id FROM pedido_venta pv
                WHERE pv.cliente_id IN (
                    SELECT id FROM cliente c
                    WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
                )
            )
        )
    );

-- Staff puede gestionar detalles de pedidos
CREATE POLICY "Staff can manage order details" ON detalle_pedido
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage order variant details" ON detalle_pedido_variante_producto
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para CÓDIGO DE DESCUENTO
-- ============================================

-- Todos pueden ver códigos de descuento activos
CREATE POLICY "Anyone can view active discount codes" ON codigo_descuento
    FOR SELECT
    TO authenticated
    USING (activo = true AND fecha_vencimiento > NOW());

-- Staff puede gestionar códigos de descuento
CREATE POLICY "Staff can manage discount codes" ON codigo_descuento
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage product discount codes" ON producto_codigo_descuento
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage order discount codes" ON detalle_pedido_codigo_descuento
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para PAGO
-- ============================================

-- Los clientes pueden ver sus propios pagos
CREATE POLICY "Clients can view own payments" ON pago
    FOR SELECT
    TO authenticated
    USING (
        pedido_id IN (
            SELECT id FROM pedido_venta pv
            WHERE pv.cliente_id IN (
                SELECT id FROM cliente c
                WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
            )
        )
    );

CREATE POLICY "Clients can view own cash payments" ON pago_efectivo
    FOR SELECT
    TO authenticated
    USING (
        pago_id IN (
            SELECT id FROM pago p
            WHERE p.pedido_id IN (
                SELECT id FROM pedido_venta pv
                WHERE pv.cliente_id IN (
                    SELECT id FROM cliente c
                    WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
                )
            )
        )
    );

CREATE POLICY "Clients can view own transfer payments" ON pago_transferencia
    FOR SELECT
    TO authenticated
    USING (
        pago_id IN (
            SELECT id FROM pago p
            WHERE p.pedido_id IN (
                SELECT id FROM pedido_venta pv
                WHERE pv.cliente_id IN (
                    SELECT id FROM cliente c
                    WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
                )
            )
        )
    );

-- Staff puede gestionar todos los pagos
CREATE POLICY "Staff can manage payments" ON pago
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage cash payments" ON pago_efectivo
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

CREATE POLICY "Staff can manage transfer payments" ON pago_transferencia
    FOR ALL
    TO authenticated
    USING (is_staff())
    WITH CHECK (is_staff());

-- ============================================
-- Políticas para DATOS DE ENTREGA Y BANCARIOS
-- ============================================

-- Los clientes pueden ver sus propios datos de entrega
CREATE POLICY "Clients can view own delivery data" ON datos_entrega
    FOR SELECT
    TO authenticated
    USING (
        cliente_id IN (
            SELECT id FROM cliente c
            WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
        )
    );

-- Los clientes pueden gestionar sus propios datos de entrega
CREATE POLICY "Clients can manage own delivery data" ON datos_entrega
    FOR ALL
    TO authenticated
    USING (
        cliente_id IN (
            SELECT id FROM cliente c
            WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
        )
    )
    WITH CHECK (
        cliente_id IN (
            SELECT id FROM cliente c
            WHERE c.id IN (SELECT id FROM usuario WHERE auth_id = auth.uid())
        )
    );

-- Solo admins pueden ver datos bancarios (información sensible)
CREATE POLICY "Admins can view bank data" ON dato_bancario
    FOR SELECT
    TO authenticated
    USING (is_admin());

-- Solo admins pueden gestionar datos bancarios
CREATE POLICY "Admins can manage bank data" ON dato_bancario
    FOR ALL
    TO authenticated
    USING (is_admin())
    WITH CHECK (is_admin());

-- ============================================
-- Verificación de políticas
-- ============================================

-- Ver todas las políticas creadas
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
