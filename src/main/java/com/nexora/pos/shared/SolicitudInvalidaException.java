package com.nexora.pos.shared;

/** Parámetros de la petición que no tienen sentido (p. ej. ordenar por un campo que no existe). El handler la traduce a HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
