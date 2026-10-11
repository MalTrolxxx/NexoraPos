package com.nexora.pos.venta;

import com.nexora.pos.producto.Producto;
import com.nexora.pos.producto.UnidadVenta;
import com.nexora.pos.shared.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las reglas del ticket, probadas en Java puro (sin Spring ni base de datos). */
class VentaTest {

    private final Producto coca = new Producto("Coca-Cola 600ml", UnidadVenta.PIEZA, new BigDecimal("18.00"), new BigDecimal("10"));
    private final Producto jamon = new Producto("Jamón de pavo", UnidadVenta.KILO, new BigDecimal("140.00"), new BigDecimal("2"));

    @Test
    void escanear_el_mismo_producto_suma_en_la_misma_linea() {
        var venta = new Venta(null);

        venta.agregar(coca, BigDecimal.ONE);
        venta.agregar(coca, BigDecimal.ONE);

        assertThat(venta.getDetalles()).hasSize(1);
        assertThat(venta.getDetalles().getFirst().getCantidad()).isEqualByComparingTo("2");
        assertThat(venta.getTotal()).isEqualByComparingTo("36.00");
    }

    @Test
    void granel_redondea_el_subtotal_al_centavo() {
        var venta = new Venta(null);

        venta.agregar(jamon, new BigDecimal("0.333"));

        assertThat(venta.getTotal()).isEqualByComparingTo("46.62");
    }

    @Test
    void no_deja_agregar_mas_de_lo_que_hay_en_stock_sumando_lo_ya_agregado() {
        var venta = new Venta(null);
        venta.agregar(coca, new BigDecimal("8"));

        assertThatThrownBy(() -> venta.agregar(coca, new BigDecimal("3")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void no_deja_vender_media_pieza() {
        var venta = new Venta(null);

        assertThatThrownBy(() -> venta.agregar(coca, new BigDecimal("0.5")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void mientras_esta_abierta_no_descuenta_stock_y_al_cobrar_si() {
        var venta = new Venta(null);
        venta.agregar(coca, new BigDecimal("3"));
        venta.agregar(jamon, new BigDecimal("0.250"));
        assertThat(coca.getStock()).isEqualByComparingTo("10");

        venta.cobrar(MetodoPago.EFECTIVO, new BigDecimal("100"));

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.PAGADA);
        assertThat(venta.getTotal()).isEqualByComparingTo("89.00");
        assertThat(venta.getCambio()).isEqualByComparingTo("11.00");
        assertThat(coca.getStock()).isEqualByComparingTo("7");
        assertThat(jamon.getStock()).isEqualByComparingTo("1.750");
    }

    @Test
    void en_efectivo_el_pago_debe_cubrir_el_total() {
        var venta = new Venta(null);
        venta.agregar(coca, new BigDecimal("2"));

        assertThatThrownBy(() -> venta.cobrar(MetodoPago.EFECTIVO, new BigDecimal("20")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no cubre");
        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.ABIERTA);
        assertThat(coca.getStock()).isEqualByComparingTo("10");
    }

    @Test
    void con_tarjeta_se_cobra_exacto_y_sin_cambio() {
        var venta = new Venta(null);
        venta.agregar(coca, BigDecimal.ONE);

        venta.cobrar(MetodoPago.TARJETA, null);

        assertThat(venta.getPagoCon()).isEqualByComparingTo("18.00");
        assertThat(venta.getCambio()).isEqualByComparingTo("0");
    }

    @Test
    void no_se_cobra_una_venta_vacia() {
        assertThatThrownBy(() -> new Venta(null).cobrar(MetodoPago.TARJETA, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void una_venta_cerrada_ya_no_se_modifica() {
        var venta = new Venta(null);
        venta.cancelar();

        assertThatThrownBy(() -> venta.agregar(coca, BigDecimal.ONE))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("CANCELADA");
    }

    @Test
    void no_se_vende_un_producto_desactivado() {
        coca.desactivar();

        assertThatThrownBy(() -> new Venta(null).agregar(coca, BigDecimal.ONE))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("desactivado");
    }
}
