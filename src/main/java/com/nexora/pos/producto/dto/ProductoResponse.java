package com.nexora.pos.producto.dto;

import com.nexora.pos.producto.Producto;

import java.math.BigDecimal;
import java.time.Instant;

/** Lo que devuelve la API. Nunca es la entidad JPA directamente (eso expondría `version` y acoplaría el contrato a Hibernate). */
public record ProductoResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        Integer stock,
        boolean activo,
        Instant creadoEn
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getPrecio(), p.getStock(), p.isActivo(), p.getCreadoEn());
    }
}
