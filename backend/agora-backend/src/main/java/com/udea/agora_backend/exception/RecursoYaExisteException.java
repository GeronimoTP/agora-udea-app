package com.udea.agora_backend.exception;

/**
 * Excepción lanzada cuando se intenta crear un recurso que ya existe
 * Ejemplo: Usuario con email duplicado, código de semillero duplicado, etc.
 */
public class RecursoYaExisteException extends RuntimeException {

    public RecursoYaExisteException(String mensaje) {
        super(mensaje);
    }

    public RecursoYaExisteException(String recurso, String campo, Object valor) {
        super(String.format("%s con %s '%s' ya existe", recurso, campo, valor));
    }
}
