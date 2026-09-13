package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfesorAreaEspecialidadResponseDTO {
    private Integer id;
    private String nombreProfesor;
    private String nombreAreaEspecialidad;
}
