package com.nexora.pos.producto;

import com.nexora.pos.shared.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas que la entidad protege por sí sola: ni Spring ni base de datos, solo Java. */
class ProductoTest {

    @Test
    void un_producto_por_pieza_no_acepta_fracciones() {
        var refresco = new Producto("Coca-Cola 600ml", UnidadVenta.PIEZA, new BigDecimal("18.00"), new BigDecimal("10"));

        assertThatThrownBy(() -> refresco.ajustarStock(new BigDecimal("-1.5")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("PIEZA");
    }

    @Test
    void un_producto_a_granel_acepta_gramos() {
        var jamon = new Producto("Jamón de pavo", UnidadVenta.KILO, new BigDecimal("140.00"), new BigDecimal("2"));

        jamon.ajustarStock(new BigDecimal("-0.250"));

        assertThat(jamon.getStock()).isEqualByComparingTo("1.750");
    }

    @Test
    void no_permite_vender_mas_de_lo_que_hay() {
        var jamon = new Producto("Jamón de pavo", UnidadVenta.KILO, new BigDecimal("140.00"), new BigDecimal("0.5"));

        assertThatThrownBy(() -> jamon.ajustarStock(new BigDecimal("-0.6")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(jamon.getStock()).isEqualByComparingTo("0.5");
    }

    @Test
    void no_permite_pasar_a_pieza_si_el_stock_tiene_fracciones() {
        var queso = new Producto("Queso Oaxaca", UnidadVenta.KILO, new BigDecimal("120.00"), new BigDecimal("1.5"));

        assertThatThrownBy(() -> queso.actualizarDatos(null, "Queso Oaxaca", UnidadVenta.PIEZA,
                new BigDecimal("120.00"), null, BigDecimal.ZERO))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void marca_bajo_stock_al_llegar_al_minimo() {
        var arroz = new Producto(null, "Arroz 1kg", UnidadVenta.PIEZA, new BigDecimal("28.00"), null,
                new BigDecimal("5"), new BigDecimal("5"));

        assertThat(arroz.isBajoStock()).isTrue();
    }
}
