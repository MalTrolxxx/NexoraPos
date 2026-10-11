CREATE TABLE cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR (120) NOT NULL,
    telefono VARCHAR (20),
    creado_en TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_cliente_telefono on cliente (telefono); -- para buscar mas rapid 