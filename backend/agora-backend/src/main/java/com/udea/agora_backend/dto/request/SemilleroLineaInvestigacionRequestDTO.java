package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SemilleroLineaInvestigacionRequestDTO {
    
    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;
    
    @NotNull(message = "El ID de la línea de investigación es obligatorio")
    private Integer idLineaInvestigacion;
}
