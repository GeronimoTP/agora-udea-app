package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;


@Data
@Builder
public class SemilleroResponseDTO {
    private Integer id;
    private String codigoIdentificador;
    private String nombre;
    private String descripcion;
    private String nombreEstudianteLider;
    private String nombrePrograma;
    private String estadoActual;
    private List<String> lineasInvestigacion;
    private String nombreFacultad;
    private Integer totalProyectos;
    private Integer totalPublicaciones;
    private Integer totalIntegrantes;
    private Boolean convocatoriaAbierta;
}
