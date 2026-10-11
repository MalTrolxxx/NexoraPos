-- Adapta el catálogo a una tienda de abarrotes:
--  * stock decimal para productos a granel (se venden por kilo: 0.250 kg de jamón)
--  * código de barras único para cobrar con lector
--  * costo y stock mínimo para saber qué resurtir y cuánto se gana
ALTER TABLE producto ADD COLUMN codigo_barras VARCHAR(50);
ALTER TABLE producto ADD COLUMN unidad VARCHAR(10) NOT NULL DEFAULT 'PIEZA';
ALTER TABLE producto ADD COLUMN costo DECIMAL(10,2);
ALTER TABLE producto ADD COLUMN stock_minimo DECIMAL(12,3) NOT NULL DEFAULT 0;

-- stock INT -> DECIMAL. H2 y MySQL no comparten sintaxis para cambiar el tipo de una columna,
-- así que se hace en pasos que ambos entienden: columna nueva, copiar, borrar la vieja, renombrar.
ALTER TABLE producto ADD COLUMN stock_decimal DECIMAL(12,3) NOT NULL DEFAULT 0;
UPDATE producto SET stock_decimal = stock;
ALTER TABLE producto DROP COLUMN stock;
ALTER TABLE producto RENAME COLUMN stock_decimal TO stock;

-- Índice único: el lector manda el código y la búsqueda es directa, sin recorrer la tabla.
-- Permite varios NULL (productos a granel sin código).
CREATE UNIQUE INDEX uk_producto_codigo_barras ON producto (codigo_barras);
