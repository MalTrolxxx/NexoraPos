package com.nexora.pos.producto;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data genera la implementación de esta interfaz en tiempo de ejecución: el nombre de
 * métodos como `existsByNombreIgnoreCase` se traduce a una consulta SQL automáticamente.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    /** Lo que usa el lector de código de barras en caja: va directo al índice único. */
    Optional<Producto> findByCodigoBarrasAndActivoTrue(String codigoBarras);

    /**
     * Búsqueda del cajero o del encargado: por parte del nombre o por código exacto, paginada para
     * no traer todo el catálogo de golpe. `q` llega ya en minúsculas desde el servicio.
     */
    @Query("""
            select p from Producto p
            where (:incluirInactivos = true or p.activo = true)
              and (:q is null or lower(p.nombre) like concat('%', :q, '%') or p.codigoBarras = :q)
            """)
    Page<Producto> buscar(String q, boolean incluirInactivos, Pageable pageable);

    /** Lo que hay que resurtir, empezando por lo más agotado. */
    @Query("select p from Producto p where p.activo = true and p.stock <= p.stockMinimo order by p.stock")
    List<Producto> findBajoStock();

    /**
     * SELECT ... FOR UPDATE: bloquea estos productos hasta que termine la transacción, para que dos cajas
     * que cobran el mismo producto a la vez se formen (milisegundos) en vez de vender stock que no existe.
     * Se bloquean siempre en orden de id: así dos cobros nunca se esperan mutuamente (deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> bloquearParaActualizar(Collection<Long> ids);
}
