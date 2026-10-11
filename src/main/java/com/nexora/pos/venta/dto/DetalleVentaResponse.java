package com.nexora.pos.venta.dto;

import com.nexora.pos.venta.DetalleVenta;

import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static DetalleVentaResponse from(DetalleVenta d) {
        // getProducto().getId() no dispara consulta: el proxy LAZY ya conoce su id.
        return new DetalleVentaResponse(d.getId(), d.getProducto().getId(), d.getNombreProducto(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal());
    }
}
