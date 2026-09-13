package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EstudianteSemilleroResponseDTO {
    private Integer id;
    private String nombreSemillero;
    private String nombreEstudiante;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;
    private String estadoVinculacion;
}
