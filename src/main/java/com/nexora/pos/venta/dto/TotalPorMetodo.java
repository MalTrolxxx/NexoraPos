package com.nexora.pos.venta.dto;

import com.nexora.pos.venta.MetodoPago;

import java.math.BigDecimal;

/** Cuánto entró por cada forma de pago: el efectivo es lo que debe haber en el cajón. */
public record TotalPorMetodo(MetodoPago metodoPago, Long ventas, BigDecimal total) {}
