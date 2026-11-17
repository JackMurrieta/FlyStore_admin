# Imágenes de productos con Supabase Storage

El formulario conserva la selección de archivo y vista previa. Envía un único POST multipart a `/api/productos`: parte `producto` (JSON con datos, variantes y precios, sin URLs) y parte `imagen` (archivo). El backend valida JPEG, PNG o WebP, máximo 5 MiB, y genera un nombre UUID. PostgreSQL conserva solamente la URL pública en `imagen_producto.url_imagen`.

## Configurar una sola vez

1. En tu proyecto Supabase, abre **Storage → New bucket**.
2. Nombre: **productos**. Activa **Public bucket** porque las fotos del catálogo son públicas.
3. Configura límite de archivo **5 MB** y tipos permitidos `image/jpeg`, `image/png`, `image/webp`. No agregues políticas de escritura pública: las subidas pasan por el backend autenticado.
4. En **Settings → API Keys**, copia una clave **secret** exclusivamente al backend. También se admite la antigua `service_role`. No uses la clave publishable para esta operación.
5. Configura las variables de entorno del proceso que inicia la API y reiníciala:

```dotenv
SUPABASE_SECRET_KEY=tu_clave_secreta
SUPABASE_STORAGE_BUCKET=productos
```

La URL del proyecto reutiliza `supabase.url`, ya configurada para la autenticación. No es la URL JDBC ni la dirección del pooler.

En NetBeans puedes configurar el entorno del proceso; alternativamente, en tu `application.properties` local ignorado por Git:

```properties
supabase.secret-key=tu_clave_secreta
supabase.storage-bucket=productos
```

Nunca pongas la clave secret/service_role en variables `VITE_`, en el frontend o en archivos versionados. No se requiere una migración adicional de tablas para Storage.

## Probar

Inicia backend y frontend. Entra con ADMIN o EMPLEADO, abre Nuevo producto, elige una foto, completa variantes y precios y guarda. Debe devolver 201, mostrar el producto y crear un objeto en `productos/catalogo/`. La BD guarda su URL, no bytes ni rutas locales del equipo.

## Capas y errores

Negocio define `IAlmacenImagenes` y coordina validación, subida y persistencia. Infraestructura implementa la comunicación HTTP con Supabase. API recibe multipart. Persistencia sigue guardando entidades y URLs mediante DAO.

Los datos inválidos y claves ya existentes se rechazan antes de subir. Si la transacción se revierte, se intenta retirar el archivo. PostgreSQL y Storage no comparten una transacción: si falla esa limpieza o el resultado del commit es desconocido, se registra el caso para reconciliación. En este último caso se conserva la imagen para evitar borrar un archivo que podría estar referenciado. El POST JSON previo sigue disponible para clientes que ya tienen URLs.

La baja de productos conserva su comportamiento anterior; no se ha añadido eliminación remota de imágenes a ese flujo pendiente.

La implementación se verifica con pruebas HTTP simuladas; la configuración del bucket y una subida real requieren tu proyecto Supabase.

Documentación oficial: [claves API](https://supabase.com/docs/guides/getting-started/api-keys), [subidas](https://supabase.com/docs/guides/storage/uploads/standard-uploads), [URLs públicas](https://supabase.com/docs/guides/storage/serving/downloads).
