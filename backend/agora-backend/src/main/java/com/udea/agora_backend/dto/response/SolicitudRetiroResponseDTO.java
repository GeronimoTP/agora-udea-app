package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class SolicitudRetiroResponseDTO {
    private Integer id;
    private Integer idSemillero;
    private String nombreSemillero;
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String motivo;
    private String estado;
    private ZonedDateTime fechaSolicitud;
    private ZonedDateTime fechaRespuesta;
    private String nombreQuienResponde;
    private String comentarioRespuesta;
}
