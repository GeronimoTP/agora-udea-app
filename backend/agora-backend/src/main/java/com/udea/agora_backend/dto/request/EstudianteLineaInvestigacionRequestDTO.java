package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EstudianteLineaInvestigacionRequestDTO {
    
    @NotNull(message = "El ID del estudiante es obligatorio")
    private Integer idEstudiante;
    
    @NotNull(message = "El ID de la línea de investigación es obligatorio")
    private Integer idLineaInvestigacion;
}
