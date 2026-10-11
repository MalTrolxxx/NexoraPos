package com.nexora.pos.venta.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Una fila de la lista de ventas en espera. Se llena directo desde la consulta JPQL (`select new ...`),
 * así que listar 30 ventas abiertas es 1 consulta y no carga ninguna línea de detalle.
 */
public record VentaResumenResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        Integer lineas,
        BigDecimal total,
        Instant creadaEn
) {}
