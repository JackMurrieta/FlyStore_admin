# Configuración de Row Level Security (RLS) con Supabase

Este documento explica cómo configurar e integrar Row Level Security (RLS) de Supabase con el backend de FlyStore.

## ¿Qué es RLS y por qué lo necesitas?

**Row Level Security (RLS)** es una característica de PostgreSQL que permite controlar qué filas de una tabla pueden ver o modificar los usuarios. En Supabase, RLS es esencial porque:

- **Seguridad en la base de datos**: Las políticas RLS se ejecutan en PostgreSQL, no en tu aplicación
- **Defensa en profundidad**: Incluso si alguien obtiene acceso directo a la base de datos, RLS protege los datos
- **Multi-tenant seguro**: Cada usuario solo puede acceder a sus propios datos

## Problema inicial: RLS bloqueando ediciones

Cuando habilitaste RLS por primera vez, probablemente experimentaste que Supabase no te dejaba editar la base de datos. Esto sucede porque:

1. RLS se habilita en las tablas → **Todas las operaciones se bloquean por defecto**
2. Necesitas crear **políticas** que definan quién puede hacer qué
3. Sin políticas, nadie (ni siquiera tú) puede acceder a los datos

## Arquitectura de la Solución

```
Cliente → JWT Token (Supabase Auth) → Spring Boot Backend → PostgreSQL con RLS
                                          ↓
                                  SupabaseJwtFilter
                                          ↓
                            DatabaseContextManager (set_config)
                                          ↓
                              PostgreSQL Session Variables
                                          ↓
                                  Políticas RLS
```

## Instalación - Fase 1: Desarrollo (Desbloquear inmediatamente)

### 1. Ejecutar script SQL temporal

Abre el **SQL Editor** en Supabase y ejecuta:

```bash
# Contenido del archivo: database/setup_rls_temporary.sql
```

Este script:
- ✅ Habilita RLS en todas las 26 tablas
- ✅ Crea políticas permisivas temporales que permiten acceso completo a usuarios autenticados
- ⚠️ **ADVERTENCIA**: Estas políticas son solo para desarrollo, NO para producción

### 2. Obtener tu JWT Secret de Supabase

1. Ve a tu proyecto en Supabase Dashboard
2. Navega a **Settings → API**
3. Copia el **JWT Secret** (aparece en la sección "JWT Settings")

### 3. Configurar variable de entorno

**Opción A - Variable de entorno del sistema:**
```bash
# Windows (CMD)
set SUPABASE_JWT_SECRET=tu-jwt-secret-aqui

# Windows (PowerShell)
$env:SUPABASE_JWT_SECRET="tu-jwt-secret-aqui"

# Linux/Mac
export SUPABASE_JWT_SECRET=tu-jwt-secret-aqui
```

**Opción B - Actualizar application.properties directamente:**
```properties
supabase.jwt.secret=tu-jwt-secret-aqui
```

⚠️ **IMPORTANTE**: Nunca subas el JWT secret a Git. Usa variables de entorno en producción.

### 4. Agregar campo auth_id a la tabla usuario

Si aún no existe, necesitas agregar el campo `auth_id` en la tabla `usuario` que almacenará el UUID de Supabase Auth:

```sql
-- Solo si no existe
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS auth_id UUID UNIQUE;

-- Crear índice para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_usuario_auth_id ON usuario(auth_id);
```

### 5. Ejecutar la aplicación

```bash
mvn spring-boot:run
```

La aplicación ahora:
- ✅ Se conecta a Supabase
- ✅ Puede crear/leer/actualizar/eliminar registros
- ✅ RLS está habilitado pero con políticas permisivas

## Instalación - Fase 2: Autenticación JWT

### Flujo de autenticación

1. **Usuario se autentica en Supabase** (desde frontend)
   ```javascript
   const { data, error } = await supabase.auth.signInWithPassword({
     email: 'usuario@ejemplo.com',
     password: 'password'
   })
   // data.session.access_token es el JWT
   ```

2. **Frontend envía peticiones con JWT**
   ```javascript
   fetch('http://tu-backend.com/api/pedidos', {
     headers: {
       'Authorization': `Bearer ${accessToken}`
     }
   })
   ```

3. **Backend valida JWT y establece contexto**
   - `SupabaseJwtFilter` extrae y valida el token
   - `DatabaseContextManager` establece variables de sesión PostgreSQL
   - Las consultas JPA ahora respetan las políticas RLS

### Probar la autenticación

1. Crea un usuario en Supabase Auth:
   ```sql
   -- En Supabase SQL Editor
   -- Esto lo hace normalmente el frontend con supabase.auth.signUp()
   ```

2. Vincula el usuario de Auth con tu tabla usuario:
   ```sql
   -- Actualizar un registro existente
   UPDATE usuario
   SET auth_id = 'uuid-del-usuario-de-supabase-auth'
   WHERE email = 'usuario@ejemplo.com';
   ```

3. Obtén un JWT token desde tu frontend o con curl:
   ```bash
   curl -X POST 'https://eydlcdaxqnmrbxsbwemm.supabase.co/auth/v1/token?grant_type=password' \
     -H "apikey: TU_ANON_KEY" \
     -H "Content-Type: application/json" \
     -d '{
       "email": "usuario@ejemplo.com",
       "password": "tu-password"
     }'
   ```

4. Usa el token para hacer peticiones:
   ```bash
   curl -X GET 'http://localhost:8080/api/pedidos' \
     -H "Authorization: Bearer eyJhbGc..."
   ```

## Instalación - Fase 3: Políticas de Producción

Cuando estés listo para implementar seguridad real:

### 1. Ejecutar script de políticas de producción

```bash
# En Supabase SQL Editor, ejecuta:
# database/setup_rls_production.sql
```

Este script:
- 🗑️ Elimina las políticas temporales permisivas
- 🔐 Implementa políticas apropiadas:
  - Usuarios ven solo sus propios datos
  - Admins tienen acceso completo
  - Clientes ven solo sus pedidos
  - Staff puede gestionar productos y pedidos
  - Datos sensibles (bancarios) solo para admins

### 2. Probar cada política

Crea tests para verificar:

```java
@Test
void clientesSoloPuedenVerSusPropiosPedidos() {
    // Setup: crear 2 clientes con pedidos diferentes
    // Test: cliente1 no debe poder ver pedidos de cliente2
}

@Test
void adminsVenTodosLosPedidos() {
    // Setup: crear admin y varios pedidos
    // Test: admin puede ver todos los pedidos
}
```

## Estructura de archivos creados

```
FlyStore_Backend/
├── API/
│   ├── src/main/java/com/flystore/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java          # Configuración de Spring Security
│   │   │   └── SupabaseProperties.java      # Propiedades de Supabase
│   │   └── security/
│   │       ├── DatabaseContextManager.java  # Gestión de contexto RLS
│   │       └── SupabaseJwtFilter.java       # Filtro de autenticación JWT
│   └── src/main/resources/
│       └── application.properties           # Actualizado con config Supabase
└── database/
    ├── setup_rls_temporary.sql             # Políticas temporales (Fase 1)
    ├── setup_rls_production.sql            # Políticas de producción (Fase 3)
    └── README_RLS_CONFIG.md                # Este archivo
```

## Solución de problemas

### Error: "new row violates row-level security policy"

**Causa**: Intentas insertar/actualizar datos pero las políticas RLS lo bloquean

**Solución**:
1. Verifica que el usuario esté autenticado (token JWT válido)
2. Revisa que la política permita la operación
3. Ejecuta las políticas temporales si estás en desarrollo

### Error: "Supabase JWT secret no está configurado"

**Causa**: El secret no está en la variable de entorno ni en application.properties

**Solución**:
```bash
export SUPABASE_JWT_SECRET=tu-secret-aqui
```

### La aplicación no puede conectarse a la base de datos

**Causa**: Problemas de conectividad o credenciales incorrectas

**Solución**:
1. Verifica las credenciales en `application.properties`
2. Asegúrate que Supabase permite conexiones desde tu IP
3. Revisa los logs de Hibernate para más detalles

### Las políticas RLS no se aplican

**Causa**: Las variables de sesión no se están estableciendo correctamente

**Solución**:
1. Verifica que `DatabaseContextManager.setUserContext()` se ejecuta
2. Revisa los logs para ver si hay errores
3. Verifica que el role "authenticated" existe en PostgreSQL:
   ```sql
   SELECT rolname FROM pg_roles WHERE rolname = 'authenticated';
   ```

## Conceptos importantes

### auth.uid() vs request.jwt.claims

- **`auth.uid()`**: Función helper que extrae el UUID del usuario desde las variables de sesión
- **`request.jwt.claims`**: Variable de sesión PostgreSQL que contiene el JSON del JWT

### Políticas USING vs WITH CHECK

- **USING**: Controla qué filas puede VER el usuario
- **WITH CHECK**: Controla qué filas puede INSERTAR/ACTUALIZAR el usuario

Ejemplo:
```sql
CREATE POLICY "example" ON pedido_venta
    FOR UPDATE
    USING (cliente_id = get_user_id())      -- Solo puede actualizar sus pedidos
    WITH CHECK (estado != 'CANCELADO');      -- No puede cambiar a CANCELADO
```

### Tipos de operaciones en políticas

- **SELECT**: Ver datos
- **INSERT**: Crear nuevos registros
- **UPDATE**: Modificar registros existentes
- **DELETE**: Eliminar registros
- **ALL**: Todas las operaciones

## Mejores prácticas

1. ✅ **Siempre usa RLS en producción** - Es tu última línea de defensa
2. ✅ **Prueba las políticas exhaustivamente** - Un error puede exponer datos
3. ✅ **Usa funciones helper** - `is_admin()`, `is_staff()` hacen las políticas más legibles
4. ✅ **Documenta tus políticas** - Futuro tú te lo agradecerá
5. ✅ **Monitorea el rendimiento** - Políticas complejas pueden ser lentas
6. ✅ **Usa índices apropiados** - Especialmente en auth_id y foreign keys

## Referencias

- [Documentación oficial de Supabase RLS](https://supabase.com/docs/guides/auth/row-level-security)
- [PostgreSQL Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Spring Security con JWT](https://spring.io/guides/tutorials/spring-boot-oauth2/)

## Siguiente pasos recomendados

1. [ ] Ejecutar `setup_rls_temporary.sql` en Supabase
2. [ ] Configurar `SUPABASE_JWT_SECRET`
3. [ ] Probar la aplicación con las políticas temporales
4. [ ] Implementar autenticación en el frontend
5. [ ] Vincular usuarios de Supabase Auth con tabla usuario
6. [ ] Probar flujo completo de autenticación
7. [ ] Ejecutar `setup_rls_production.sql`
8. [ ] Crear tests para verificar políticas
9. [ ] Desplegar a producción

¿Necesitas ayuda? Abre un issue en el repositorio o consulta la documentación de Supabase.
