package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VinculacionProyectoRequestDTO {

    @NotNull(message = "El ID del proyecto es obligatorio")
    private Integer idProyecto;

    @NotNull(message = "El ID del estudiante es obligatorio")
    private Integer idEstudiante;

    @NotNull(message = "El rol dentro del proyecto es obligatorio")
    private Integer idRolProyecto;
    
    // La fecha de vinculación se puede generar automáticamente en el Service con LocalDate.now()
}
