package com.nexora.pos.venta.dto;

/** El cliente es opcional: la mayoría de las ventas de mostrador son a "público en general". */
public record AbrirVentaRequest(Long clienteId) {}
