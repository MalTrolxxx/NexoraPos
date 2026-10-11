package com.nexora.pos.producto;

import com.nexora.pos.producto.dto.ProductoRequest;
import com.nexora.pos.shared.ReglaDeNegocioException;
import com.nexora.pos.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la capa de servicio: aquí NO se levanta Spring ni base de datos real.
 * El repositorio se simula con Mockito para probar solo las reglas de negocio, rápido y aislado.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    private static ProductoRequest agua(String codigo) {
        return new ProductoRequest(codigo, "Agua 600ml", UnidadVenta.PIEZA, new BigDecimal("15.00"), null, new BigDecimal("10"), null);
    }

    @Test
    void no_permite_crear_un_producto_con_nombre_repetido() {
        when(repository.existsByNombreIgnoreCase("Agua 600ml")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(agua(null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Agua 600ml");

        verify(repository, never()).save(any());
    }

    @Test
    void no_permite_crear_un_producto_con_codigo_repetido() {
        when(repository.existsByCodigoBarras("7501055300075")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(agua("7501055300075")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("7501055300075");

        verify(repository, never()).save(any());
    }

    @Test
    void crea_el_producto_con_stock_minimo_en_cero_si_no_se_indica() {
        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        var creado = service.crear(agua("  "));

        assertThat(creado.nombre()).isEqualTo("Agua 600ml");
        assertThat(creado.codigoBarras()).isNull();
        assertThat(creado.stock()).isEqualByComparingTo("10");
        assertThat(creado.stockMinimo()).isEqualByComparingTo("0");
    }

    @Test
    void no_permite_renombrar_un_producto_con_el_nombre_de_otro() {
        var producto = new Producto("Agua 1L", UnidadVenta.PIEZA, new BigDecimal("20.00"), BigDecimal.ONE);
        when(repository.findById(1L)).thenReturn(Optional.of(producto));
        when(repository.existsByNombreIgnoreCaseAndIdNot("Agua 600ml", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1L, agua(null)))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(producto.getNombre()).isEqualTo("Agua 1L");
    }

    @Test
    void registrar_entrada_suma_al_stock() {
        var producto = new Producto("Agua 600ml", UnidadVenta.PIEZA, new BigDecimal("15.00"), new BigDecimal("3"));
        when(repository.findById(1L)).thenReturn(Optional.of(producto));

        var actualizado = service.registrarEntrada(1L, new BigDecimal("24"));

        assertThat(actualizado.stock()).isEqualByComparingTo("27");
    }

    @Test
    void lanza_no_encontrado_si_el_id_no_existe() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
