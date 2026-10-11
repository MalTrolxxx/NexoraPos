CREATE TABLE venta (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    cliente_id  BIGINT,
    estado      VARCHAR(10)   NOT NULL,
    -- Se guarda (y no se calcula al vuelo) para listar ventas y sacar el corte sin leer los detalles.
    total       DECIMAL(12,2) NOT NULL DEFAULT 0,
    metodo_pago VARCHAR(15),
    pago_con    DECIMAL(12,2),
    cambio      DECIMAL(12,2),
    creada_en   TIMESTAMP     NOT NULL,
    cerrada_en  TIMESTAMP,
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

-- Sirve para las dos consultas más frecuentes: "ventas abiertas" (estado) y "corte del día" (estado + rango de fecha).
CREATE INDEX ix_venta_estado_cerrada ON venta (estado, cerrada_en);
CREATE INDEX ix_venta_cliente ON venta (cliente_id);

CREATE TABLE detalle_venta (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    venta_id        BIGINT        NOT NULL,
    producto_id     BIGINT        NOT NULL,
    -- Copia del nombre y precio al momento de vender: si mañana cambia el precio, el ticket de hoy no cambia.
    nombre_producto VARCHAR(120)  NOT NULL,
    cantidad        DECIMAL(12,3) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_venta FOREIGN KEY (venta_id) REFERENCES venta (id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto (id)
);

CREATE INDEX ix_detalle_venta ON detalle_venta (venta_id);
CREATE INDEX ix_detalle_producto ON detalle_venta (producto_id);
