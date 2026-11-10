package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tiempo promedio entre la oferta del líder y la decisión del estudiante. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TiempoPromedioRespuestaDTO {
    private Integer totalPostulacionesEvaluadas;
    private Double promedioHoras;
    private Double promedioDias;
}
