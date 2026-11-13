# FlyStore Backend

Backend Java 21 y Spring Boot 3.2.5, separado en cinco proyectos Maven. Cada proyecto tiene su propio pom.xml, fuentes y JAR. API es el único proyecto ejecutable; la interfaz visual permanece en el frontend existente.

| Proyecto | Clases y responsabilidades |
| --- | --- |
| Persistencia | Entidades JPA, enums persistidos, conexión y pool, configuración de BD, DAO e interfaces, repositorios Spring Data |
| Negocio | BO e interfaces, mappers, resultados internos, excepciones de negocio y contrato de autenticación externa |
| Infraestructura | Adaptador Supabase Auth, DTOs externos, RestClient y propiedades del proveedor |
| DTOs | Solicitudes y respuestas públicas de la API |
| API | Controladores REST, seguridad HTTP, cookies, traducción de errores y arranque |

La dependencia principal es API → Negocio → Persistencia. Negocio usa las interfaces DAO de Persistencia y transforma entidades a resultados/DTO mediante sus mappers. Persistencia no depende de Negocio, API ni Infraestructura. Infraestructura implementa el contrato de autenticación externa definido en Negocio; API la incorpora con alcance runtime.

El antiguo proyecto Entidades fue integrado en Persistencia. No hay un artefacto de entidades separado. Los cinco proyectos se compilan desde el POM raíz y cada uno genera su JAR.

Paquetes principales:

- Persistencia: com.flystore.persistencia.entidades, dao, dao.interfaces, repositorios y conexion.
- Negocio: com.flystore.modulos.autenticacion.aplicacion, sus subpaquetes modelo y puertos, com.flystore.modulos.catalogo.aplicacion y sus mappers, y com.flystore.dominio.excepcion.
- Infraestructura: adaptador Supabase y configuración de su cliente HTTP.
- API: presentación REST y com.flystore.presentacion.seguridad.

ProductoMapper y CatalogoMapper viven en Negocio. ProductoBO y CatalogoBO implementan el catálogo de los diagramas. La autenticación anterior se conserva; se retiraron UsuarioBO y los mappers de autenticación agregados durante la refactorización. Los tokens se entregan mediante cookies HttpOnly.

ConexionBD construye el DataSource Hikari en Persistencia. Las propiedades JDBC/JPA viven en Persistencia/src/main/resources/persistencia-prod.properties; el perfil prod de API las importa desde el classpath. Las credenciales se reciben mediante variables de entorno y no están escritas en el repositorio. Se conservan los IDs Long de persistencia y se exponen como String en los DTO. El catálogo requiere la migración manual Persistencia/migraciones/001_catalogo.sql para agregar clave y permitir imágenes sin color. Esta migración no se ejecuta automáticamente.

## Catálogo implementado

Ver [CATALOGO.md](CATALOGO.md) para las rutas, el contrato de alta, la migración pendiente y las reglas aplicadas. El backend del catálogo sigue los cuatro diagramas adjuntos; el frontend existente no fue modificado.

## Relación con los diagramas

Se revisaron `../StarUml/flyStore.mdj` y `../StarUml/diagramFly.mdj`. Contienen modelos de análisis y diseño con variantes; no deben implementarse todas las clases repetidas como entidades nuevas. Ver [la guía de implementación](ARQUITECTURA.md).

## Compilación y ejecución

Requisitos: JDK 21 y Maven 3.9 o superior.

```powershell
mvn clean verify
java -jar API/target/API-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

Abrir el `pom.xml` raíz en el IDE para cargar todos los proyectos. Para verificar una capa y sus dependencias, usar por ejemplo `mvn -pl Negocio -am clean verify` o `mvn -pl Persistencia -am clean verify`. Los cuatro proyectos de biblioteca generan JAR normales; el ejecutable de API incluye esos JAR como dependencias.

Configurar las siguientes variables de entorno antes de arrancar con el perfil `prod`:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<database>
SPRING_DATASOURCE_USERNAME=<usuario>
SPRING_DATASOURCE_PASSWORD=<contraseña>
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://<proyecto>.supabase.co/auth/v1
SUPABASE_JWKS_URL=https://<proyecto>.supabase.co/auth/v1/.well-known/jwks.json
SUPABASE_URL=https://<proyecto>.supabase.co
SUPABASE_PUBLISHABLE_KEY=<clave-publicable>
SUPABASE_SECRET_KEY=<clave-servidor>
APP_BACKEND_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:5173
APP_COOKIE_SECURE=false
```

En producción usar HTTPS y `APP_COOKIE_SECURE=true`. `APP_COOKIE_DOMAIN` es opcional. Las cookies conservan `SameSite=Strict`: frontend y backend deben desplegarse en el mismo sitio para que funcione la sesión. La política CSRF existente continúa desactivada; revisar esa decisión si se cambia a cookies entre sitios.

Las rutas `/api/auth/**` validan credenciales y tokens dentro del caso de uso. `/health` es una sonda pública del proceso, no de conexión a la base. `/api/admin/**` requiere ADMIN o EMPLEADO; las demás rutas requieren autenticación. Al incorporar consultas públicas de catálogo hay que declararlas explícitamente en `SecurityConfig`.

Las pruebas de negocio y mapeo de registro viven en Negocio, las de conexión y mapeo JPA en Persistencia y las de REST, seguridad y arquitectura en API. Verifican también la ubicación física de las clases y las dependencias entre POM. Se ejecutan sin una base ni un proyecto Supabase y no sustituyen las pruebas de integración contra el esquema real. Docker compila todos los proyectos con `mvn clean verify` y utiliza `/health` como sonda.
