package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfesorAreaEspecialidadRequestDTO {
    
    @NotNull(message = "El ID del profesor es obligatorio")
    private Integer idProfesor;
    
    @NotNull(message = "El ID del área de especialidad es obligatorio")
    private Integer idAreaEspecialidad;
}
