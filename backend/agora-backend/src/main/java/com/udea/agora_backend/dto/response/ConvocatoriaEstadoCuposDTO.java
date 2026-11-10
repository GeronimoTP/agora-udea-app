package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para respuesta de estado de cupos
 */
@Data
@Builder
public class ConvocatoriaEstadoCuposDTO {
    private Integer idConvocatoria;
    private String nombreConvocatoria;
    private Integer cuposOriginales;
    private Integer cuposDisponibles;
    private Integer cuposUtilizados;
    private Double porcentajeOcupacion;
}
