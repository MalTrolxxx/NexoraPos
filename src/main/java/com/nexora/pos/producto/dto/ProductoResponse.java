package com.nexora.pos.producto.dto;

import com.nexora.pos.producto.Producto;
import com.nexora.pos.producto.UnidadVenta;

import java.math.BigDecimal;
import java.time.Instant;

/** Lo que devuelve la API. Nunca es la entidad JPA directamente (eso expondría `version` y acoplaría el contrato a Hibernate). */
public record ProductoResponse(
        Long id,
        String codigoBarras,
        String nombre,
        UnidadVenta unidad,
        BigDecimal precio,
        BigDecimal costo,
        BigDecimal stock,
        BigDecimal stockMinimo,
        boolean bajoStock,
        boolean activo,
        Instant creadoEn
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigoBarras(), p.getNombre(), p.getUnidad(), p.getPrecio(),
                p.getCosto(), p.getStock(), p.getStockMinimo(), p.isBajoStock(), p.isActivo(), p.getCreadoEn());
    }
}
