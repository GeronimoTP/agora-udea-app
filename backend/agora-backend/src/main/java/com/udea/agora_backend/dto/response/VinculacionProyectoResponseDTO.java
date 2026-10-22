package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class VinculacionProyectoResponseDTO {
    private Integer id;
    private String tituloProyecto;
    private Integer idEstudiante;
    private String nombreEstudiante;
    private Integer idRolProyecto;
    private String rolProyecto;
    private LocalDate fechaVinculacion;
}
