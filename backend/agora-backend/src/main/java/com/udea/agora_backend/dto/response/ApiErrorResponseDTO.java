package com.udea.agora_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO estandarizado para respuestas de error en la API.
 * Se usa en GlobalExceptionHandler para todas las excepciones.
 */
@Data
@Builder
@AllArgsConstructor
public class ApiErrorResponseDTO {
    
    private Integer status;
    
    private String error;
    
    private String message;
    
    private String path;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime timestamp;
    
    // Para errores de validación de campo
    private List<FieldErrorDTO> fieldErrors;
    
    /**
     * DTO auxiliar para errores de validación por campo
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class FieldErrorDTO {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
