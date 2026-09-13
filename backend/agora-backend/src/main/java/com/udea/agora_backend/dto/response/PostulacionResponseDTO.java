package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class PostulacionResponseDTO {
    private Integer id;
    private String tituloConvocatoria;
    private String nombreEstudiante;
    private String respuestaMotivacion;
    private Integer disponibilidadHorasSemana;
    private String experienciaPrevia;
    private LocalDate fechaPostulacion;
    private ZonedDateTime fechaDecisionEstudiante;
    private String estadoPostulacion;
    private List<String> habilidades;
}
