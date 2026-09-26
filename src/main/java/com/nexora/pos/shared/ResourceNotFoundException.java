package com.nexora.pos.shared;

/** Se lanza cuando se busca por id (o clave) algo que no existe. El handler global la traduce a HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
