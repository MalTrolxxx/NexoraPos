package com.nexora.pos.cliente;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Test
    void existsByTelefonoAndIdNot_excluye_al_propio_cliente() {
        Cliente ana = repository.save(new Cliente("Ana", "5551234"));

        assertThat(repository.existsByTelefono("5551234")).isTrue();
        assertThat(repository.existsByTelefonoAndIdNot("5551234", ana.getId())).isFalse();
    }

    @Test
    void varios_clientes_sin_telefono_no_chocan_con_el_indice_unico() {
        repository.saveAndFlush(new Cliente("Ana", null));
        repository.saveAndFlush(new Cliente("Luis", null));

        assertThat(repository.count()).isEqualTo(2);
    }
}
