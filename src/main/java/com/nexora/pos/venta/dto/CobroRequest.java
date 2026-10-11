package com.nexora.pos.venta.dto;

import com.nexora.pos.venta.MetodoPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** `pagoCon` solo importa en EFECTIVO (con cuánto pagó el cliente, para calcular el cambio). */
public record CobroRequest(
        @NotNull(message = "El método de pago es obligatorio (EFECTIVO, TARJETA o TRANSFERENCIA)")
        MetodoPago metodoPago,

        @Positive(message = "El pago debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "El pago admite máximo 2 decimales")
        BigDecimal pagoCon
) {}
