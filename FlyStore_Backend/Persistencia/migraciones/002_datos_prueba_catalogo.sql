-- FLYSTORE: datos de prueba para tablas existentes y VACÍAS.
-- Ejecutar después de 001_catalogo.sql. No ejecutar si se utilizó 000_esquema_inicial.sql.
-- No crea ni elimina tablas, usuarios o contraseñas. Una repetición aborta sin duplicar datos.
BEGIN;
SET LOCAL search_path TO public;
LOCK TABLE categoria, subcategoria, marca, tipo_talla, talla, color,
           producto, variante_producto, precio_producto IN SHARE ROW EXCLUSIVE MODE;

DO $comprobar$
BEGIN
    IF EXISTS (SELECT 1 FROM categoria)
       OR EXISTS (SELECT 1 FROM subcategoria)
       OR EXISTS (SELECT 1 FROM marca)
       OR EXISTS (SELECT 1 FROM tipo_talla)
       OR EXISTS (SELECT 1 FROM talla)
       OR EXISTS (SELECT 1 FROM color)
       OR EXISTS (SELECT 1 FROM producto)
       OR EXISTS (SELECT 1 FROM variante_producto)
       OR EXISTS (SELECT 1 FROM precio_producto) THEN
        RAISE EXCEPTION 'Las tablas de catálogo ya contienen datos. Se canceló la carga para evitar duplicados; no se modificó ningún registro.';
    END IF;
END
$comprobar$;

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

-- Resultado esperado: DEMO-001, DEMO-002 y DEMO-003.
SELECT p.id, p.clave, p.nombre, p.estado_producto,
       (SELECT COALESCE(SUM(v.stock), 0) FROM public.variante_producto v WHERE v.producto_id = p.id) AS existencias
FROM public.producto p
ORDER BY p.id;