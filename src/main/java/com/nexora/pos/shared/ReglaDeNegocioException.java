package com.nexora.pos.shared;

/**
 * Se lanza cuando una operación es válida en la forma (los datos están bien tipados y presentes)
 * pero viola una regla del negocio: nombre repetido, stock insuficiente, venta ya cobrada, etc.
 * El handler global la traduce a HTTP 409 (Conflict).
 */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String message) {
        super(message);
    }
}
