package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Demanda de una convocatoria: postulaciones recibidas por cada cupo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandaConvocatoriaDTO {
    private Integer idConvocatoria;
    private String titulo;
    private String estado;
    private Integer cuposTotales;
    private Integer cuposDisponibles;
    private Long postulacionesRecibidas;
    private Double tasaDemanda;
}
