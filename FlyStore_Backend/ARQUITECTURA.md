# Estructura por proyectos y guía para los diagramas

## Distribución de clases

| Clase o responsabilidad | Proyecto | Paquete o ubicación |
| --- | --- | --- |
| Cliente, Usuario, Producto, PedidoVenta, Pago y demás entidades/enums | Persistencia | com.flystore.persistencia.entidades |
| IClienteDAO, IEmpleadoDAO, IAdminDAO, ITipoClienteDAO | Persistencia | com.flystore.persistencia.dao.interfaces |
| ClienteDAO, EmpleadoDAO, AdminDAO, TipoClienteDAO | Persistencia | com.flystore.persistencia.dao |
| Repositorios Spring Data | Persistencia | com.flystore.persistencia.repositorios |
| ConexionBD y configuración JDBC/JPA | Persistencia | com.flystore.persistencia.conexion y resources/persistencia-prod.properties |
| AuthBO e IAuthBO (preexistentes); ProductoBO, IProductoBO, CatalogoBO, ICatalogoBO | Negocio | com.flystore.modulos.autenticacion.aplicacion |
| ProductoMapper, CatalogoMapper | Negocio | com.flystore.modulos.catalogo.aplicacion.mappers |
| Resultados internos y contrato de autenticación externa | Negocio | aplicacion.modelo y aplicacion.puertos |
| NegocioException y TipoError | Negocio | com.flystore.dominio.excepcion |
| SupabaseAuthGateway y DTOs específicos del proveedor | Infraestructura | modulos.autenticacion.infraestructura.supabase |
| RestClientConfig y SupabaseProperties | Infraestructura | com.flystore.infraestructura.config |
| DTOs públicos | DTOs | com.flystore.contratos.autenticacion |
| Controladores REST, seguridad HTTP, cookies y errores HTTP | API | presentación |
| Pantallas, hooks, ViewModel y modelos MVVM | Frontend existente | No se generan estas clases en el backend |

## Dependencias de compilación

- Persistencia no depende de otro proyecto interno.
- DTOs no depende de otro proyecto interno.
- Negocio depende de Persistencia y DTOs.
- Infraestructura depende de Negocio para implementar su contrato de autenticación externa.
- API depende de Negocio y DTOs; incorpora Infraestructura como dependencia runtime.

No hay dependencia de Persistencia hacia Negocio. Las interfaces DAO se publican desde Persistencia y los BO las consumen por inyección de constructor. Los controladores de catálogo utilizan interfaces BO. Se preservó la consulta de roles de autenticación anterior en SupabaseJwtConverter y se retiró UsuarioBO, añadido durante la refactorización.

## Incorporar casos de uso de los diagramas

Se revisaron ../StarUml/flyStore.mdj y ../StarUml/diagramFly.mdj. Los modelos contienen variantes y clases repetidas de análisis; seleccionar el escenario definitivo antes de implementarlo.

1. Ubicar las entidades y enums del caso en Persistencia. Reutilizar Producto, Cliente, PedidoVenta y demás clases existentes; no duplicarlas por caso de uso.
2. Agregar las interfaces DAO en Persistencia y sus implementaciones junto a los repositorios. Estas clases realizan consultas y escrituras, sin importar BO ni DTOs públicos.
3. Definir solicitudes/respuestas en DTOs con validaciones de formato.
4. Crear el BO y su interfaz en Negocio. Allí pertenecen GestorVenta, ConsultarCatalogo, AdministrarCatalogo y AdministrarPedidos. El BO coordina reglas y transacciones públicas y consume interfaces DAO.
5. Crear los mappers correspondientes en Negocio para convertir entidades, DTOs y resultados. Las decisiones de negocio permanecen en el BO y las consultas en el DAO.
6. Si hay una integración externa, definir su contrato en Negocio y su adaptador en Infraestructura. Los DTOs del proveedor permanecen en el adaptador.
7. Exponer la operación mediante un controlador de API y configurar su autorización. Mantener la lógica de interfaz visual en el frontend.
8. Probar cada capa y ejecutar mvn clean verify desde la raíz.

## Conexión y transacciones

ConexionBD crea el DataSource y aplica las propiedades spring.datasource y spring.datasource.hikari. La configuración de producción de API importa persistencia-prod.properties desde el JAR de Persistencia. Los valores sensibles provienen del entorno.

JPA mantiene ddl-auto=none y open-in-view=false. Las entidades nuevas requieren revisar el esquema y preparar una migración explícita. Convertir las asociaciones LAZY a DTO dentro de la transacción de negocio cuando corresponda. Las anotaciones transaccionales deben estar en métodos públicos invocados mediante el proxy de Spring.

La transacción local no incluye Supabase. Un registro remoto seguido de una escritura local fallida requiere una política de compensación o reconciliación antes de ampliar ese escenario. Se conserva el resultado null para registro pendiente por compatibilidad con el controlador existente.

## Validación y alcance

Las pruebas comprueban ubicación física de clases, dependencias Maven, acceso de Negocio a contratos DAO, ausencia de acceso directo de los controladores de catálogo a Persistencia, descubrimiento de componentes entre JAR, mapeos JPA, construcción del pool sin abrir conexiones, autenticación y autorización. No sustituyen la integración contra PostgreSQL y Supabase reales.

El proyecto Entidades fue absorbido por Persistencia. Docker compila los cinco proyectos y empaqueta sus dependencias en el ejecutable de API. Las rutas, nombres JSON y cookies de autenticación se conservan. El catálogo administrativo está implementado según los diagramas adjuntos; venta y pedidos quedan pendientes. Los DAO de catálogo usan EntityManager directamente y no agregan repositorios, BO o servicios ajenos a esos diagramas. Los DAO y repositorios anteriores de autenticación permanecen.
