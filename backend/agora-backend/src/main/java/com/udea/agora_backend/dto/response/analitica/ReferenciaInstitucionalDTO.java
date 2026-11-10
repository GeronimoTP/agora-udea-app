package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Valores institucionales con los que un líder compara los indicadores de su semillero. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenciaInstitucionalDTO {
    private Double tasaDemandaPromedio;
    private Double porcentajeRetencion;
    private Double promedioHorasRespuesta;
    private Double promedioPublicacionesPorProyecto;
    private Double porcentajeParticipacionEstudiantil;
    private Double porcentajeInterdisciplinariedad;
}
