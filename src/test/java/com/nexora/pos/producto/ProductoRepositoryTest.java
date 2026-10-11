package com.nexora.pos.producto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    private Producto coca;

    @BeforeEach
    void catalogo() {
        coca = repository.save(new Producto("7501055300075", "Coca-Cola 600ml", UnidadVenta.PIEZA,
                new BigDecimal("18.00"), new BigDecimal("13.50"), new BigDecimal("24"), new BigDecimal("6")));
        repository.save(new Producto(null, "Coca-Cola 2L", UnidadVenta.PIEZA,
                new BigDecimal("38.00"), null, new BigDecimal("2"), new BigDecimal("4")));
        repository.save(new Producto("Frijol a granel", UnidadVenta.KILO, new BigDecimal("32.00"), new BigDecimal("12.500")));
        Producto descontinuado = new Producto("Coca-Cola Light", UnidadVenta.PIEZA, new BigDecimal("18.00"), BigDecimal.ZERO);
        descontinuado.desactivar();
        repository.save(descontinuado);
    }

    @Test
    void existsByNombreIgnoreCase_ignora_mayusculas_y_minusculas() {
        assertThat(repository.existsByNombreIgnoreCase("coca-cola 600ML")).isTrue();
        assertThat(repository.existsByNombreIgnoreCase("Pepsi")).isFalse();
    }

    @Test
    void buscar_por_parte_del_nombre_solo_trae_activos_y_pagina() {
        var pagina = repository.buscar("coca", false, PageRequest.of(0, 1, Sort.by("nombre")));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(Producto::getNombre).containsExactly("Coca-Cola 2L");
    }

    @Test
    void buscar_puede_incluir_inactivos() {
        var pagina = repository.buscar("coca", true, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
    }

    @Test
    void buscar_por_codigo_exacto() {
        var pagina = repository.buscar("7501055300075", false, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Producto::getId).containsExactly(coca.getId());
    }

    @Test
    void el_lector_encuentra_por_codigo_de_barras() {
        assertThat(repository.findByCodigoBarrasAndActivoTrue("7501055300075")).contains(coca);
    }

    @Test
    void bajo_stock_trae_lo_que_hay_que_resurtir() {
        assertThat(repository.findBajoStock()).extracting(Producto::getNombre).containsExactly("Coca-Cola 2L");
    }

    @Test
    void stock_decimal_se_guarda_sin_perder_gramos() {
        var frijol = repository.findAll().stream().filter(p -> p.getUnidad() == UnidadVenta.KILO).findFirst().orElseThrow();
        frijol.ajustarStock(new BigDecimal("-0.750"));
        repository.flush();

        assertThat(repository.findById(frijol.getId()).orElseThrow().getStock()).isEqualByComparingTo("11.750");
    }
}
