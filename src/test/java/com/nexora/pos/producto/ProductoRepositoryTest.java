package com.nexora.pos.producto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración ligera: levanta solo la capa JPA + una H2 en memoria + las migraciones
 * de Flyway reales, para confirmar que el mapeo de la entidad contra el esquema de verdad es correcto.
 */
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repository;

    @Test
    void existsByNombreIgnoreCase_ignora_mayusculas_y_minusculas() {
        repository.save(new Producto("Agua 600ml", new BigDecimal("15.00"), 20));

        assertThat(repository.existsByNombreIgnoreCase("agua 600ML")).isTrue();
        assertThat(repository.existsByNombreIgnoreCase("Refresco")).isFalse();
    }
}
