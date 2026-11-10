package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Reconocimientos agrupados por entidad otorgante y por tipo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistribucionImpactoReconocimientosDTO {
    private Integer totalReconocimientos;
    private Map<String, Long> distribucionPorEntidad;
    private Map<String, Long> distribucionPorTipo;
}
