package com.nexora.pos.cliente;

import com.nexora.pos.cliente.dto.ClienteRequest;
import com.nexora.pos.shared.ReglaDeNegocioException;
import com.nexora.pos.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    @Test
    void no_permite_crear_un_cliente_con_telefono_repetido() {
        when(repository.existsByTelefono("5551234")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new ClienteRequest("Ana", "5551234")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("5551234");

        verify(repository, never()).save(any());
    }

    @Test
    void telefono_vacio_se_guarda_como_null() {
        when(repository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        var creado = service.crear(new ClienteRequest("Ana", "   "));

        assertThat(creado.telefono()).isNull();
        verify(repository, never()).existsByTelefono(any());
    }

    @Test
    void actualizar_permite_conservar_el_propio_telefono() {
        var cliente = new Cliente("Ana", "5551234");
        when(repository.findById(1L)).thenReturn(Optional.of(cliente));
        when(repository.existsByTelefonoAndIdNot("5551234", 1L)).thenReturn(false);

        var actualizado = service.actualizar(1L, new ClienteRequest("Ana Maria", "5551234"));

        assertThat(actualizado.nombre()).isEqualTo("Ana Maria");
    }

    @Test
    void lanza_no_encontrado_si_el_id_no_existe() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
