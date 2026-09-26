package com.nexora.pos.producto;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data genera la implementación de esta interfaz en tiempo de ejecución, incluyendo
 * `existsByNombreIgnoreCase`: el nombre del método se traduce a una consulta SQL automáticamente.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
}
