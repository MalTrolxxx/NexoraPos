package com.nexora.pos.producto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
