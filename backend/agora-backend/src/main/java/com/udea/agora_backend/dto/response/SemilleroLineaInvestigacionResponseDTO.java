package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SemilleroLineaInvestigacionResponseDTO {
    private Integer id;
    private String nombreSemillero;
    private String nombreLineaInvestigacion;
}
