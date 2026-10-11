package com.nexora.pos.venta.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CantidadRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 9, fraction = 3, message = "La cantidad admite máximo 3 decimales")
        BigDecimal cantidad
) {}
