package com.udea.agora_backend.exception;

import com.udea.agora_backend.dto.response.ApiErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador global de excepciones para la aplicación.
 * Intercepta todas las excepciones lanzadas por los controladores y servicios,
 * y devuelve respuestas JSON estandarizadas con código HTTP apropiado.
 */
@ControllerAdvice
public class ManejadorExcepcionesGlobal extends ResponseEntityExceptionHandler {

    /**
     * Maneja RecursoNoEncontradoException (404 Not Found)
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleRecursoNoEncontradoException(
            RecursoNoEncontradoException ex,
            WebRequest request) {

        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso No Encontrado")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja ConflictoException (409 Conflict)
     * Se lanza cuando intenta crear un recurso duplicado
     */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleConflictoException(
            ConflictoException ex,
            WebRequest request) {

        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Conflicto")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Maneja ExcepcionValidacion (400 Bad Request)
     * Se lanza por errores de validación de lógica de negocio
     */
    @ExceptionHandler(ExcepcionValidacion.class)
    public ResponseEntity<ApiErrorResponseDTO> handleExcepcionValidacion(
            ExcepcionValidacion ex,
            WebRequest request) {

        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de Validación")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja ErrorLogicaNegocioException (422 Unprocessable Entity)
     * Se lanza cuando la operación viola reglas de negocio
     */
    @ExceptionHandler(ErrorLogicaNegocioException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleErrorLogicaNegocioException(
            ErrorLogicaNegocioException ex,
            WebRequest request) {

        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .status(422)  // UNPROCESSABLE_ENTITY
                .error("Error en Lógica de Negocio")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Maneja errores de validación de Jakarta Validation (@NotNull, @Email, etc.)
     * Se lanza automáticamente cuando falla la validación en el request body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            org.springframework.http.HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        List<ApiErrorResponseDTO.FieldErrorDTO> fieldErrors = new ArrayList<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.add(
                        ApiErrorResponseDTO.FieldErrorDTO.builder()
                                .field(error.getField())
                                .message(error.getDefaultMessage())
                                .rejectedValue(error.getRejectedValue())
                                .build()
                )
        );

        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Error en validación de campos")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(ZonedDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones genéricas no capturadas (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleGlobalException(
            Exception ex,
            WebRequest request) {

        ApiErrorResponseDTO errorResponse = ApiErrorResponseDTO.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Ocurrió un error inesperado en el servidor")
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(ZonedDateTime.now())
                .build();

        ex.printStackTrace();  // Log en consola (en producción usar Logger)

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
