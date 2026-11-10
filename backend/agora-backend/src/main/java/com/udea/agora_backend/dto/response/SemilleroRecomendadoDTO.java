package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para respuesta del motor de recomendación
 */
@Data
@Builder
public class SemilleroRecomendadoDTO {
    private Integer idSemillero;
    private String nombreSemillero;
    private String codigoIdentificador;
    private String descripcion;
    private String nombreEstudianteLider;
    private String nombrePrograma;
    private Double porcentajeMatch;
    private Double factor1Habilidades;
    private Double factor2AreaEspecialidad;
    private Double factor3DisponibilidadConvocatorias;
    private Double factor4ProximidadTematica;
}
