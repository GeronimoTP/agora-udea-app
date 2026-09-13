package com.udea.agora_backend.exception;

/**
 * Excepción lanzada cuando ocurre un error en la lógica de negocio.
 * Ejemplo: No se pueden aceptar más estudiantes (cupos llenos),
 * Un estudiante intenta postularse en una convocatoria ya cerrada, etc.
 * Mapea a HTTP 422 Unprocessable Entity
 */
public class ErrorLogicaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErrorLogicaNegocioException(String message) {
        super(message);
    }

    public ErrorLogicaNegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
