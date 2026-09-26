CREATE TABLE producto (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(120)  NOT NULL,
    precio      DECIMAL(10,2) NOT NULL,
    stock       INT           NOT NULL DEFAULT 0,
    activo      BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMP     NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_producto_nombre ON producto (nombre);
