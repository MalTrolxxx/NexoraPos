package com.nexora.pos.cliente.dto;

import com.nexora.pos.cliente.Cliente;
import java.time.Instant;

public record ClienteResponse(
        Long id,
        String nombre,
        String telefono,
        Instant creadoEn) {
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getTelefono(), c.getCreadoEn());
    }
}
