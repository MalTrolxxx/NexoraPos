package com.nexora.pos.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Lo que llega en el body al crear o actualizar un producto. Un record: es inmutable y solo transporta datos. */
public record ProductoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @NotNull(message = "El stock inicial es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock
) {}
