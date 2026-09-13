package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstudianteLineaInvestigacionResponseDTO {
    private Integer id;
    private String nombreEstudiante;
    private String nombreLineaInvestigacion;
}
