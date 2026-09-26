# NexoraPOS — Sistema de Punto de Venta

Stack: Java 21 + Spring Boot 4.1.1 + Spring Data JPA + Flyway + H2 (dev) / MySQL (producción)

Proyecto de aprendizaje: se construye módulo por módulo, cada uno con su propia
migración, entidad, repositorio, servicio, controlador y tests — siguiendo siempre
el mismo patrón para que sea fácil de replicar y de revisar.

## Idea central: ventas en espera

A diferencia de un POS simple (una venta = un cobro inmediato), aquí una venta
puede quedar **abierta** mientras el cliente sigue en la tienda: si se le olvidó
algo, se le agrega a la misma venta abierta en vez de tener que hacer un cobro
aparte. Un cliente puede tener varias ventas abiertas al mismo tiempo (varias
"sesiones"); el stock solo se descuenta hasta que la venta se cobra.

```
Venta
 ├─ estado: ABIERTA | PAGADA | CANCELADA
 ├─ cliente (opcional)
 └─ DetalleVenta[] (producto, cantidad, precio unitario al momento de agregarlo)
```

## Módulos

| Módulo     | Estado       |
|------------|--------------|
| Producto   | ✅ Hecho (entidad, CRUD, tests) |
| Cliente    | 🔜 En construcción |
| Venta      | 🔜 Pendiente (depende de Producto y Cliente) |

## Arquitectura

Paquete por feature (`producto/`, `cliente/`, `venta/`), cada uno en capas:

```
Controller  → traduce HTTP, sin lógica de negocio
Service     → reglas de negocio, transacciones (@Transactional)
Repository  → acceso a datos (Spring Data JPA)
Entity      → protege sus propias invariantes (sin setters sueltos)
dto/        → lo que entra y sale por la API (nunca la entidad JPA directa)
```

El esquema de base de datos vive en `src/main/resources/db/migration` (Flyway).
Hibernate nunca crea ni modifica tablas (`ddl-auto=validate`): si la entidad no
coincide con las migraciones, la app no arranca. Así el esquema queda versionado
igual que el código.

## Desarrollo

Requiere JDK 21.

```bash
./mvnw spring-boot:run      # http://localhost:8080
./mvnw test                 # tests unitarios + integración (H2 en memoria)
```

Consola de la base de datos (H2, solo en desarrollo): http://localhost:8080/h2-console
JDBC URL: `jdbc:h2:file:./data/pos` — usuario `sa`, sin contraseña.

Los datos de desarrollo viven en `./data/` (no se versiona; cada quien tiene la suya).
