package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EstudianteSemilleroResponseDTO {
    private Integer id;
    private String nombreSemillero;
    private Integer idSemillero;
    private Integer idEstudiante;
    private Integer idUsuario;
    private String nombreEstudiante;
    private String email;
    private String nombrePrograma;
    private Integer semestre;
    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;
    private String estadoVinculacion;
}
