# Preparación de una base nueva en Supabase

1. Abrir `000_esquema_inicial.sql`, copiar todo y pegarlo en una consulta nueva del SQL Editor de Supabase.
2. Ejecutar la consulta completa con el rol `postgres`. El script crea las tablas actuales, relaciones, tipos enum, el índice de clave y tres productos identificados como prueba. No crea usuarios de autenticación.
3. El último resultado debe mostrar `DEMO-001`, `DEMO-002` y `DEMO-003`, con estados PUBLICADO, BORRADOR e INACTIVO.
4. Después configurar Supabase Auth, vincular un usuario de prueba con `usuario` y `admin`, configurar el backend y arrancar ambos proyectos.

El script inicial ya incorpora el cambio de `001_catalogo.sql`. Para una instalación nueva se usa **solo `000_esquema_inicial.sql`**. `001_catalogo.sql` se reserva para bases anteriores que ya contienen las tablas.

Si las tablas ya existen pero el catálogo está vacío, aplicar `001_catalogo.sql` y después `002_datos_prueba_catalogo.sql`. Este último carga las mismas categorías, tallas, colores y tres productos de demostración sin crear tablas. Se detiene si las tablas de catálogo ya contienen datos, evitando duplicados. No usarlo después del inicial `000`, que ya incluye esos datos.

El inicial se detiene si detecta una tabla existente de FlyStore. No contiene borrado ni reemplazo de tablas o datos. Si falla, revisar el error antes de intentar otra cosa; no eliminar tablas para forzar su ejecución.

Las tablas tienen RLS habilitado sin políticas de acceso público. El backend actual se conecta por JDBC con el usuario de conexión del proyecto y aplica la autorización en Spring Security. Los datos no se consultan directamente desde el navegador con las claves públicas de Supabase.

El DDL se genera desde las entidades con Hibernate 6.4 y la misma estrategia de nombres físicos que Spring Boot. `EsquemaInicialTest` comprueba la correspondencia del DDL sin conectar a una BD. La ejecución real del SQL en Supabase debe verificarse con el resultado de la consulta.

Para regenerar el DDL tras modificar entidades, desde la raíz:

```powershell
mvn -pl Persistencia -am '-Dtest=EsquemaInicialTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dflystore.exportarEsquema=true' test
```

El resultado queda en `Persistencia/target/esquema-generado.sql`. Actualizar únicamente el bloque DDL delimitado del inicial, revisar los datos de prueba y ejecutar la prueba sin la propiedad de exportación.
