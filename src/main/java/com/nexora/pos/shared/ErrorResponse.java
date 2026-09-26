package com.nexora.pos.shared;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/** Forma única de respuesta de error para toda la API, para que el frontend no tenga que adivinar. */
public record ErrorResponse(Instant timestamp, int status, String error, String message, Map<String, String> detalles) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> detalles) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, detalles);
    }
}
