package com.udea.agora_backend.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no es encontrado en la base de datos.
 * Mapea a HTTP 404 Not Found
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    public RecursoNoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecursoNoEncontradoException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s : '%s'", resourceName, fieldName, fieldValue));
    }

    public RecursoNoEncontradoException(String resourceName, Integer id) {
        super(String.format("%s con ID %d no encontrado", resourceName, id));
    }
}
