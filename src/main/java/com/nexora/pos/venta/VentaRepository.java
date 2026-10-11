package com.nexora.pos.venta;

import com.nexora.pos.venta.dto.TotalPorMetodo;
import com.nexora.pos.venta.dto.VentaResumenResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Venta + cliente + líneas en una sola consulta (JOIN), en vez de 1 + N consultas. */
    @EntityGraph(attributePaths = {"cliente", "detalles"})
    Optional<Venta> findConDetallesById(Long id);

    @Query("""
            select new com.nexora.pos.venta.dto.VentaResumenResponse(v.id, c.id, c.nombre, size(v.detalles), v.total, v.creadaEn)
            from Venta v left join v.cliente c
            where v.estado = :estado and (:clienteId is null or c.id = :clienteId)
            order by v.creadaEn
            """)
    List<VentaResumenResponse> resumenPorEstado(EstadoVenta estado, Long clienteId);

    /** El corte se suma en la base de datos (GROUP BY): no importa si fueron 10 o 10,000 ventas, viaja una fila por método. */
    @Query("""
            select new com.nexora.pos.venta.dto.TotalPorMetodo(v.metodoPago, count(v), sum(v.total))
            from Venta v
            where v.estado = :estado and v.cerradaEn >= :desde and v.cerradaEn < :hasta
            group by v.metodoPago
            order by v.metodoPago
            """)
    List<TotalPorMetodo> totalesPorMetodo(EstadoVenta estado, Instant desde, Instant hasta);
}
