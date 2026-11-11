package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class SemilleroResponseDTO {
    private Integer id;
    private String codigoIdentificador;
    private String nombre;
    private String descripcion;
    private Integer idEstudianteLider;
    private Integer idUsuarioLider;
    private String nombreEstudianteLider;
    private Integer idPrograma;
    private String nombrePrograma;
    private String estadoActual;
    private List<String> lineasInvestigacion;
    private String nombreFacultad;
    private Integer totalProyectos;
    private Integer totalPublicaciones;
    private Integer totalIntegrantes;
    private Boolean convocatoriaAbierta;
    // Aprobación del administrador: cuándo se solicitó (o reenvió), cuándo se revisó y, si se
    // rechazó, por qué. Nulos en los semilleros anteriores a la aprobación.
    private ZonedDateTime fechaSolicitud;
    private ZonedDateTime fechaRevision;
    private String motivoRechazo;
}
