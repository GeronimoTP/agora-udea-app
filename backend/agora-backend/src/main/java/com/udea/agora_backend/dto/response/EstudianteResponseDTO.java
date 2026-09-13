package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class EstudianteResponseDTO {
    private Integer id;
    private String nombreCompleto;
    private String email;
    private String nombrePrograma;
    private Integer semestre;
    private ZonedDateTime createdAt;
    private List<String> lineasInvestigacion;
}
