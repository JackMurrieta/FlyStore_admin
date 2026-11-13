# Catálogo administrativo

Implementación de los cuatro diagramas proporcionados, distribuida en los proyectos existentes. Se conserva la autenticación, los DAO anteriores y las entidades previas. Se retiraron UsuarioBO, IUsuarioBO, AuthMapper y ClienteMapper, añadidos durante la refactorización y ausentes de estos diagramas; su comportamiento de autenticación se conserva en las clases anteriores.

## Clases del catálogo

- Persistencia: ProductoDAO, CategoriaDAO, SubcategoriaDAO, MarcaDAO, TallaDAO, DetallePedidoDAO y sus seis interfaces. Usan EntityManager, sin añadir repositorios o servicios no diagramados.
- Negocio: ProductoBO/IProductoBO, CatalogoBO/ICatalogoBO, ProductoMapper y CatalogoMapper.
- DTOs: ProductoCatalogoDTO, CrearProductoDTO, VarianteDTO, PrecioProductoDTO, CatalogoItemDTO, FiltrosDisponiblesDTO, TallaDTO y ResultadoBajaDTO.
- API: ProductoController y CatalogoController.

Los identificadores de entidades y métodos DAO son Long. Los BO públicos y DTOs reciben o devuelven String. Los importes usan BigDecimal. Las tallas y colores del alta son nombres; se resuelven contra los registros existentes sin crear catálogos implícitamente. Las tallas deben pertenecer a la categoría seleccionada. Las imágenes son URLs del producto, sin color obligatorio.

## Migración previa al despliegue

Revisar y ejecutar manualmente Persistencia/migraciones/001_catalogo.sql contra PostgreSQL antes de desplegar esta versión. No se ha ejecutado sobre ninguna base real.

El script agrega producto.clave, asigna LEGACY-<id> a productos antiguos sin clave, exige clave y crea un índice único insensible a mayúsculas y espacios exteriores. También permite imagen_producto.color_id nulo. No cambia los IDs ni elimina registros. Las claves LEGACY pueden sustituirse manualmente por las claves comerciales deseadas. La migración aborta si los datos existentes violan el índice único.

JPA mantiene ddl-auto=none. El código no aplica cambios de esquema automáticamente.

## Rutas REST

Todas requieren autenticación y rol ADMIN o EMPLEADO mediante la regla existente /api/admin/**.

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | /api/admin/productos | 200 y lista ProductoCatalogoDTO |
| GET | /api/admin/productos/buscar | 200 y lista filtrada |
| POST | /api/admin/productos | 201 sin cuerpo |
| DELETE | /api/admin/productos/{idProducto} | 200 y ResultadoBajaDTO |
| GET | /api/admin/catalogo/filtros | 200 y FiltrosDisponiblesDTO |
| GET | /api/admin/catalogo/categorias/{idCategoria}/tallas | 200 y lista TallaDTO |

La búsqueda admite texto, idCategoria, idSubcategoria, idMarca y estado como parámetros opcionales. El texto busca por nombre o clave sin distinguir mayúsculas. Los filtros se combinan. El catálogo administrativo puede mostrar borradores e inactivos; no constituye un catálogo público.

## Alta

POST recibe application/json con este contrato:

```json
{
  "clave": "GORRA-001",
  "nombre": "Gorra roja",
  "descripcion": "Gorra bordada",
  "idCategoria": "1",
  "idSubcategoria": "2",
  "idMarca": "3",
  "imagenes": ["https://ejemplo.com/gorra.jpg"],
  "estado": "PUBLICADO",
  "variantes": [{"talla": "M", "color": "Rojo", "stock": 5}],
  "preciosProducto": [{"tipoPrecio": "BASE", "precio": 150.00, "minPiezas": 1}]
}
```

El alta guarda el agregado completo en una transacción. La subcategoría debe pertenecer a la categoría, la marca debe existir y estar activa, y cada nombre de talla/color debe resolverse sin ambigüedad. Se genera un SKU técnico único por variante porque el esquema anterior lo requiere; no es un dato solicitado en el diagrama.

Validaciones: clave/nombre/descripción obligatorios, IDs positivos dentro del rango Long, imágenes HTTP/HTTPS, estados PUBLICADO/BORRADOR/INACTIVO, al menos una variante e imagen, stock no negativo y combinaciones talla/color únicas ignorando mayúsculas y espacios exteriores.

Se requiere al menos un precio, tipos de precio válidos y no repetidos, importes y cantidades mínimos positivos, y BASE desde una pieza. Los precios por volumen no pueden superar BASE. Se preservan los valores de TipoPrecio anteriores. El estado STOCK_BAJO preexistente se conserva en la entidad y puede consultarse, pero no se acepta como nuevo estado de alta según el diagrama.

La clave se valida antes de guardar y el índice único protege también ante altas simultáneas. Los errores de validación devuelven 400, los recursos faltantes 404 y los conflictos de integridad/clave 409.

## Baja

Si existen detalles de venta relacionados con variantes del producto, se cambia su estado a INACTIVO y se devuelve tipoBaja LOGICA. Sin ventas se elimina y se devuelve FISICA. El ID de respuesta es String. La operación bloquea el producto durante la transacción. Una referencia ajena a ventas que impida eliminarlo se informa como conflicto 409; no se eliminan registros históricos para forzar la operación.

## Alcance de las pruebas

Se prueban reglas de alta/baja, filtros e IDs, mapeo del agregado, contratos y validación HTTP, preservación de sesión, seguridad y composición entre proyectos. Se valida el metamodelo JPA y la interpretación de las consultas HQL sin abrir conexiones. Falta ejecutar la migración y las pruebas de integración contra el esquema PostgreSQL real.

El frontend existente usa actualmente contratos de alta distintos (File y pasos separados). No se modificó: la integración deberá enviar el JSON completo con URLs que especifica este diagrama. Este backend no añade carga de archivos, edición, reactivación ni endpoints de venta fuera del alcance mostrado.
