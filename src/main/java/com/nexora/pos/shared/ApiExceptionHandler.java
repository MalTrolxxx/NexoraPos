package com.nexora.pos.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un solo lugar donde las excepciones se convierten en respuestas HTTP.
 * Gracias a esto, los controladores no llevan try/catch: lanzan la excepción y aquí se decide el status code.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaDeNegocioException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Dos cajeros modificaron lo mismo a la vez (@Version): el segundo debe reintentar con datos frescos. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "Otro usuario modificó este registro al mismo tiempo; vuelve a intentarlo");
    }

    /**
     * Red de seguridad: los servicios validan los datos únicos antes de guardar, pero si dos peticiones
     * llegan al mismo tiempo ambas pasan la validación y el índice único de la BD frena a la segunda.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridad(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT, "El dato ya existe o choca con otro registro");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "Datos inválidos", errores));
    }

    /** JSON mal formado o un valor que no existe en un enum (p. ej. "unidad": "LITRO"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, mensaje));
    }
}
