package com.nexora.pos.shared;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Respuesta paginada estable para la API. No devolvemos `Page` de Spring directamente porque su
 * forma JSON es un detalle interno que puede cambiar entre versiones.
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <T> Pagina<T> de(Page<T> page) {
        return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
