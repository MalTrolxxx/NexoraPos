package com.nexora.pos.venta;

/** ABIERTA: el cliente sigue comprando (se pueden agregar/quitar productos). PAGADA y CANCELADA ya no cambian. */
public enum EstadoVenta {
    ABIERTA,
    PAGADA,
    CANCELADA
}
