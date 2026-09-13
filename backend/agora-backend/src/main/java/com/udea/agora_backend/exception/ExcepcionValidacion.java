package com.udea.agora_backend.exception;

/**
 * Excepción lanzada cuando existe un error en la validación de datos de negocio.
 * Usada para errores de validación lógica en lugar de Jakarta validation.
 * Mapea a HTTP 400 Bad Request
 */
public class ExcepcionValidacion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ExcepcionValidacion(String message) {
        super(message);
    }

    public ExcepcionValidacion(String message, Throwable cause) {
        super(message, cause);
    }
}
