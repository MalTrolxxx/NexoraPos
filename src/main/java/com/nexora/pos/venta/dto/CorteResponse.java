package com.nexora.pos.venta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Corte de caja de un día: solo cuenta ventas PAGADAS. */
public record CorteResponse(
        LocalDate fecha,
        long ventas,
        BigDecimal total,
        BigDecimal ticketPromedio,
        List<TotalPorMetodo> porMetodo
) {}
