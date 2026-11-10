package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tasa de demanda de un semillero: postulaciones recibidas por cada cupo ofertado. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TasaDemandaSemilleroDTO {
    private Integer idSemillero;
    private String nombreSemillero;
    private Integer cuposOfertados;
    private Long postulacionesRecibidas;
    private Double tasaDemanda;
}
