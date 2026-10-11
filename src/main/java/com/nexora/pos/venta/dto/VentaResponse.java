package com.nexora.pos.venta.dto;

import com.nexora.pos.cliente.Cliente;
import com.nexora.pos.venta.EstadoVenta;
import com.nexora.pos.venta.MetodoPago;
import com.nexora.pos.venta.Venta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** La venta completa, con sus líneas: lo que se muestra en caja y lo que se imprime como ticket. */
public record VentaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        EstadoVenta estado,
        List<DetalleVentaResponse> detalles,
        BigDecimal total,
        MetodoPago metodoPago,
        BigDecimal pagoCon,
        BigDecimal cambio,
        Instant creadaEn,
        Instant cerradaEn
) {
    public static VentaResponse from(Venta v) {
        Cliente c = v.getCliente();
        return new VentaResponse(v.getId(), c == null ? null : c.getId(), c == null ? null : c.getNombre(), v.getEstado(),
                v.getDetalles().stream().map(DetalleVentaResponse::from).toList(),
                v.getTotal(), v.getMetodoPago(), v.getPagoCon(), v.getCambio(), v.getCreadaEn(), v.getCerradaEn());
    }
}
