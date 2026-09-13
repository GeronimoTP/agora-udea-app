package com.udea.agora_backend.exception;

/**
 * Excepción lanzada cuando existe un conflicto en la operación solicitada.
 * Por ejemplo: intentar crear un recurso que ya existe, email duplicado, etc.
 * Mapea a HTTP 409 Conflict
 */
public class ConflictoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictoException(String message) {
        super(message);
    }

    public ConflictoException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictoException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s con %s '%s' ya existe", resourceName, fieldName, fieldValue));
    }
}
