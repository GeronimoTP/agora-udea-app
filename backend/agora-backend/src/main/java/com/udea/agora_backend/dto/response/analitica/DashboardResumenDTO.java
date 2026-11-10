package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Resumen de todos los indicadores institucionales (vista del administrador). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResumenDTO {
    private Long postulacionesActivas;
    private List<TasaDemandaSemilleroDTO> tasasDemanda;
    private TiempoPromedioRespuestaDTO tiempoRespuesta;
    private TasaRetencionOfertasDTO tasaRetencion;
    private ConversionProyectoPublicacionDTO conversionProyectos;
    private ParticipacionEstudiantilDTO participacionEstudiantil;
    private DistribucionImpactoReconocimientosDTO impactoReconocimientos;
    private List<HabilidadFrecuenciaDTO> habilidadesEmergentes;
    private InterdisciplinariedadDTO interdisciplinariedad;
}
