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

## Pensado para abarrotes

- **Lector de código de barras**: `GET /api/productos/codigo/{codigo}` va directo a un índice único.
- **Granel**: cada producto se vende por `PIEZA` o por `KILO`. El stock es decimal (gramos),
  y la entidad rechaza cosas sin sentido como vender 1.5 refrescos.
- **Búsqueda rápida**: `GET /api/productos?q=coca&page=0&size=20`, por parte del nombre o por código, siempre paginada.
- **Resurtido**: `POST /api/productos/{id}/entradas` y `GET /api/productos/bajo-stock`.

## Flujo en caja

```
POST /api/ventas                          {"clienteId": 3}            ← opcional; sin body = público en general
POST /api/ventas/{id}/detalles            {"codigoBarras": "750..."}  ← cada escaneo (cantidad 1 por defecto)
POST /api/ventas/{id}/detalles            {"productoId": 8, "cantidad": 0.250}   ← granel
PUT  /api/ventas/{id}/detalles/{linea}    {"cantidad": 3}
DELETE /api/ventas/{id}/detalles/{linea}
POST /api/ventas/{id}/cobro               {"metodoPago": "EFECTIVO", "pagoCon": 200}   → devuelve el cambio
POST /api/ventas/{id}/cancelar
GET  /api/ventas/abiertas?clienteId=3     ← ventas en espera
GET  /api/ventas/corte?fecha=2026-10-11   ← corte de caja (sin fecha = hoy)
```

## Decisiones de rendimiento

| Problema | Solución |
|---|---|
| Buscar el producto escaneado | Índice único en `codigo_barras`: búsqueda directa, no recorre la tabla |
| Catálogos grandes | Toda búsqueda es paginada (máximo 100 por página) |
| N+1 consultas al abrir una venta | `@EntityGraph`: venta + cliente + líneas en una sola consulta |
| Listar ventas en espera | Proyección `select new ...` a DTO con el total ya guardado: no carga las líneas |
| Corte de caja | `GROUP BY` en la base de datos + índice `(estado, cerrada_en)` |
| Dos cajas cobrando el mismo producto | `SELECT ... FOR UPDATE` en orden de id: se forman, no se venden existencias fantasma ni hay deadlocks |
| Dos personas editando lo mismo | `@Version` (bloqueo optimista) → 409 en vez de pisar cambios |
| Relaciones LAZY recorridas en ciclo | `default_batch_fetch_size=50`: carga en lotes como red de seguridad |
| "Hoy" en el servidor ≠ "hoy" en la tienda | `nexora.zona-horaria` define el día del corte |

## Módulos

| Módulo     | Estado       |
|------------|--------------|
| Producto   | ✅ Hecho (código de barras, granel por kilo, búsqueda paginada, resurtido, bajo stock) |
| Cliente    | ✅ Hecho (alta/edición/consulta, tests) |
| Venta      | ✅ Hecho (ventas en espera, granel, cobro con cambio, cancelación, corte de caja) |

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
