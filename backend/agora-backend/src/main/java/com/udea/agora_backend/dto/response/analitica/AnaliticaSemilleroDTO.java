package com.udea.agora_backend.dto.response.analitica;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Indicadores de un semillero para sus líderes y tutores, con la referencia institucional. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnaliticaSemilleroDTO {
    private Integer idSemillero;
    private String nombreSemillero;
    private Integer integrantesActivos;
    private Integer integrantesRetirados;
    private Long postulacionesActivas;
    private Long totalPostulaciones;
    private Map<String, Long> postulacionesPorEstado;
    private Double tasaDemanda;
    private List<DemandaConvocatoriaDTO> demandaPorConvocatoria;
    private TiempoPromedioRespuestaDTO tiempoRespuesta;
    private TasaRetencionOfertasDTO tasaRetencion;
    private ConversionProyectoPublicacionDTO conversionProyectos;
    private ParticipacionEstudiantilDTO participacionEstudiantil;
    private DistribucionImpactoReconocimientosDTO impactoReconocimientos;
    private List<HabilidadFrecuenciaDTO> habilidadesEmergentes;
    private InterdisciplinariedadDTO interdisciplinariedad;
    private ReferenciaInstitucionalDTO referenciaInstitucional;
}
