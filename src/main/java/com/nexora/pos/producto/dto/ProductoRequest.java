package com.nexora.pos.producto.dto;

import com.nexora.pos.producto.UnidadVenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Lo que llega en el body al crear o actualizar un producto. Un record: es inmutable y solo transporta datos.
 * Los @Digits reflejan el tamaño de las columnas, para rechazar con 400 lo que la BD no podría guardar.
 */
public record ProductoRequest(

        @Size(max = 50, message = "El código de barras admite máximo 50 caracteres")
        String codigoBarras,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite máximo 120 caracteres")
        String nombre,

        @NotNull(message = "La unidad es obligatoria (PIEZA o KILO)")
        UnidadVenta unidad,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El precio admite máximo 2 decimales")
        BigDecimal precio,

        @PositiveOrZero(message = "El costo no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo admite máximo 2 decimales")
        BigDecimal costo,

        /** Solo se usa al crear (si no viene, es 0). Después el stock cambia con entradas y ventas, nunca editando el producto. */
        @PositiveOrZero(message = "El stock no puede ser negativo")
        @Digits(integer = 9, fraction = 3, message = "El stock admite máximo 3 decimales")
        BigDecimal stockInicial,

        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        @Digits(integer = 9, fraction = 3, message = "El stock mínimo admite máximo 3 decimales")
        BigDecimal stockMinimo
) {}
