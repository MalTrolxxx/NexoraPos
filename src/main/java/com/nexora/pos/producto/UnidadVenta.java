package com.nexora.pos.producto;

import java.math.BigDecimal;

/** Cómo se vende un producto: por pieza (refresco, jabón) o a granel por kilo (frijol, queso, jamón). */
public enum UnidadVenta {
    PIEZA,
    KILO;

    /** Una pieza no se puede partir; un kilo admite hasta gramos (3 decimales, igual que la columna). */
    public boolean admite(BigDecimal cantidad) {
        int decimales = cantidad.stripTrailingZeros().scale();
        return this == PIEZA ? decimales <= 0 : decimales <= 3;
    }
}
