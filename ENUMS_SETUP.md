# Configuración de Enums en PostgreSQL/Supabase

## Resumen

El proyecto Java usa enums personalizados que necesitan sincronizarse con los tipos ENUM de PostgreSQL en Supabase.

## Cambios Realizados

### 1. Enums creados en Supabase (via `create_enums.sql`)

- `color` - Colores disponibles
- `talla` - Tallas generales y New Era
- `tipo_usuario` - Tipos de usuario (cliente, admin, empleado)
- `tipo_cliente` - Tipos de cliente
- `tipo_precio` - Tipos de precio
- `estado_pago` - Estados de pago
- `estado_venta` - Estados de venta

### 2. Cambios en las Entidades JPA

Se reemplazó `@Enumerated(EnumType.STRING)` por `@Convert` para usar los converters personalizados:

#### Antes:
```java
@Enumerated(EnumType.STRING)
@Column(name = "color", nullable = false)
private Color color;
```

#### Después:
```java
@Convert(converter = Entity.converter.ColorConverter.class)
@Column(name = "color", nullable = false, columnDefinition = "color")
private Color color;
```

### 3. Entidades Modificadas

1. **VarianteProducto.java** (líneas 28-34)
   - Campo `color` - usa ColorConverter
   - Campo `talla` - usa TallaConverter

2. **Cliente.java** (líneas 23-25)
   - Campo `tipoCliente` - usa TipoClienteConverter

3. **PedidoVenta.java** (líneas 33-35)
   - Campo `estadoVenta` - usa EstadoVentaConverter

4. **Pago.java** (líneas 38-40)
   - Campo `estadoPago` - usa EstadoPagoConverter

5. **PrecioProducto.java** (líneas 30-32)
   - Campo `tipoPrecio` - usa TipoPrecioConverter

6. **DetallePedido.java** (líneas 38-40)
   - Campo `tipoPrecio` - usa TipoPrecioConverter

## Cómo Funciona

### Los Converters

Cada converter implementa `AttributeConverter<EnumJava, String>`:

```java
@Converter(autoApply = false)
public class ColorConverter implements AttributeConverter<Color, String> {

    @Override
    public String convertToDatabaseColumn(Color color) {
        return color.getValor(); // Convierte NEGRO -> "negro"
    }

    @Override
    public Color convertToEntityAttribute(String valor) {
        return Color.fromValor(valor); // Convierte "negro" -> NEGRO
    }
}
```

### Flujo de Datos

**Java → BD:**
```
Color.NEGRO → converter → "negro" → tipo enum color en PostgreSQL
```

**BD → Java:**
```
tipo enum color "negro" → converter → Color.NEGRO
```

## Ventajas de este Enfoque

1. **Type Safety en BD**: PostgreSQL valida que solo se inserten valores válidos del enum
2. **Claridad**: Los valores en BD son legibles ("negro" en vez de "NEGRO")
3. **Consistencia**: Los enums Java usan el método `getValor()` para obtener el string correcto
4. **Migración Fácil**: Si cambias el nombre del enum Java, solo actualizas el converter

## Nota sobre Usuario.tipo_usuario

La tabla `usuarios` usa `@DiscriminatorColumn` para herencia SINGLE_TABLE, por lo que el campo `tipo_usuario` se maneja automáticamente por JPA y no necesita converter. Los valores serán los de `@DiscriminatorValue` en cada clase hija (Cliente, Admin, Empleado).

## Comandos Útiles en Supabase

### Verificar enums creados:
```sql
SELECT typname FROM pg_type WHERE typtype = 'e' ORDER BY typname;
```

### Ver valores de un enum:
```sql
SELECT unnest(enum_range(NULL::color));
SELECT unnest(enum_range(NULL::talla));
SELECT unnest(enum_range(NULL::tipo_cliente));
SELECT unnest(enum_range(NULL::tipo_precio));
SELECT unnest(enum_range(NULL::estado_pago));
SELECT unnest(enum_range(NULL::estado_venta));
```

### Agregar nuevo valor a un enum existente:
```sql
ALTER TYPE color ADD VALUE 'turquesa';
```

### Eliminar y recrear un enum:
```sql
DROP TYPE color CASCADE;
CREATE TYPE color AS ENUM ('negro', 'blanco', 'azul', ...);
```
