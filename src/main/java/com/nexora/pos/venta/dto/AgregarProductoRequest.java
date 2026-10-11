package com.nexora.pos.venta.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * El producto se indica por id (lo eligieron en la búsqueda) o por código de barras (lo escanearon).
 * Si no viene cantidad se agrega 1, que es lo normal al escanear.
 */
public record AgregarProductoRequest(
        Long productoId,

        String codigoBarras,

        @Positive(message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite máximo 3 decimales")
        BigDecimal cantidad
) {
    @AssertTrue(message = "Indica productoId o codigoBarras")
    public boolean isProductoIndicado() {
        return productoId != null || (codigoBarras != null && !codigoBarras.isBlank());
    }
}
