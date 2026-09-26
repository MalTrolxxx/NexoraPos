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

    @Test
    void no_permite_crear_un_producto_con_nombre_repetido() {
        when(repository.existsByNombreIgnoreCase("Agua 600ml")).thenReturn(true);

        var request = new ProductoRequest("Agua 600ml", new BigDecimal("15.00"), 10);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Agua 600ml");

        verify(repository, never()).save(any());
    }

    @Test
    void crea_el_producto_cuando_el_nombre_no_existe() {
        when(repository.existsByNombreIgnoreCase("Agua 600ml")).thenReturn(false);
        when(repository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new ProductoRequest("Agua 600ml", new BigDecimal("15.00"), 10);
        var creado = service.crear(request);

        assertThat(creado.nombre()).isEqualTo("Agua 600ml");
        assertThat(creado.stock()).isEqualTo(10);
    }

    @Test
    void lanza_no_encontrado_si_el_id_no_existe() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
