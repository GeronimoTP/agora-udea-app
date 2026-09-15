package com.udea.agora_backend.service;

import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVICIO 3: Módulo de Analítica e Indicadores de Impacto Institucional (Dashboard)
 * 
 * Calcula métricas institucionales de atracción, productividad científica,
 * impacto de reconocimientos, tendencias de habilidades e interdisciplinariedad.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnaliticaService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final PostulacionRepository postulacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final PublicacionRepository publicacionRepository;
    private final PublicacionUsuarioRepository publicacionUsuarioRepository;
    private final ReconocimientoSemilleroRepository reconocimientoRepository;
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final SemilleroRepository semilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteRepository estudianteRepository;

    /**
     * 1. Tasa de Demanda por Semillero: Total postulaciones recibidas / Total cupos ofertados
     */
    public List<TasaDemandaSemilleroDTO> calcularTasaDemandaPorSemillero() {
        List<Semillero> semilleros = semilleroRepository.findAll();
        List<Convocatoria> convocatorias = convocatoriaRepository.findAll();
        List<Postulacion> postulaciones = postulacionRepository.findAll();

        List<TasaDemandaSemilleroDTO> resultado = new ArrayList<>();

        for (Semillero s : semilleros) {
            List<Convocatoria> convocatoriasSemillero = convocatorias.stream()
                    .filter(c -> c.getSemillero() != null && c.getSemillero().getId().equals(s.getId()))
                    .toList();

            int cuposTotales = convocatoriasSemillero.stream()
                    .mapToInt(Convocatoria::getCuposTotales)
                    .sum();

            long totalPostulaciones = postulaciones.stream()
                    .filter(p -> p.getConvocatoria() != null && p.getConvocatoria().getSemillero() != null
                            && p.getConvocatoria().getSemillero().getId().equals(s.getId()))
                    .count();

            double tasaDemanda = cuposTotales > 0 ? (double) totalPostulaciones / cuposTotales : 0.0;

            resultado.add(TasaDemandaSemilleroDTO.builder()
                    .idSemillero(s.getId())
                    .nombreSemillero(s.getNombre())
                    .cuposOfertados(cuposTotales)
                    .postulacionesRecibidas(totalPostulaciones)
                    .tasaDemanda(Math.round(tasaDemanda * 100.0) / 100.0)
                    .build());
        }

        return resultado;
    }

    /**
     * 2. Tiempo Promedio de Respuesta del Estudiante (en horas / días)
     */
    public TiempoPromedioRespuestaDTO calcularTiempoPromedioRespuesta() {
        List<Postulacion> postulacionesDecididas = postulacionRepository.findAll().stream()
                .filter(p -> p.getFechaDecisionEstudiante() != null && p.getCreatedAt() != null)
                .toList();

        if (postulacionesDecididas.isEmpty()) {
            return TiempoPromedioRespuestaDTO.builder()
                    .totalPostulacionesEvaluadas(0)
                    .promedioHoras(0.0)
                    .promedioDias(0.0)
                    .build();
        }

        double totalHoras = 0.0;
        for (Postulacion p : postulacionesDecididas) {
            Duration duration = Duration.between(p.getCreatedAt(), p.getFechaDecisionEstudiante());
            totalHoras += Math.max(0, duration.toHours());
        }

        double promedioHoras = totalHoras / postulacionesDecididas.size();
        double promedioDias = promedioHoras / 24.0;

        return TiempoPromedioRespuestaDTO.builder()
                .totalPostulacionesEvaluadas(postulacionesDecididas.size())
                .promedioHoras(Math.round(promedioHoras * 100.0) / 100.0)
                .promedioDias(Math.round(promedioDias * 100.0) / 100.0)
                .build();
    }

    /**
     * 3. Tasa de Retención vs. Rechazo de Ofertas
     */
    public TasaRetencionOfertasDTO calcularTasaRetencionRechazo() {
        List<Postulacion> postulaciones = postulacionRepository.findAll();

        long aceptadas = postulaciones.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().getNombre().equalsIgnoreCase("Aceptada"))
                .count();

        long rechazadasEstudiante = postulaciones.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().getNombre().equalsIgnoreCase("Rechazada por Estudiante"))
                .count();

        long expiradas = postulaciones.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().getNombre().equalsIgnoreCase("Expirada"))
                .count();

        long totalOfertasEmitidas = aceptadas + rechazadasEstudiante + expiradas;

        double tasaRetencion = totalOfertasEmitidas > 0 ? ((double) aceptadas / totalOfertasEmitidas) * 100.0 : 0.0;
        double tasaRechazo = totalOfertasEmitidas > 0 ? (((double) (rechazadasEstudiante + expiradas)) / totalOfertasEmitidas) * 100.0 : 0.0;

        return TasaRetencionOfertasDTO.builder()
                .ofertasAceptadas(aceptadas)
                .ofertasRechazadasPorEstudiante(rechazadasEstudiante)
                .ofertasExpiradas(expiradas)
                .totalOfertasProcesadas(totalOfertasEmitidas)
                .porcentajeRetencion(Math.round(tasaRetencion * 100.0) / 100.0)
                .porcentajeRechazoYExpiracion(Math.round(tasaRechazo * 100.0) / 100.0)
                .build();
    }

    /**
     * 4. Índice de Conversión Proyecto-Publicación
     */
    public ConversionProyectoPublicacionDTO calcularConversionProyectoPublicacion() {
        List<Proyecto> proyectos = proyectoRepository.findAll();
        List<Publicacion> publicaciones = publicacionRepository.findAll();

        long totalProyectos = proyectos.size();
        long totalPublicaciones = publicaciones.size();

        double promedio = totalProyectos > 0 ? (double) totalPublicaciones / totalProyectos : 0.0;

        return ConversionProyectoPublicacionDTO.builder()
                .totalProyectos(totalProyectos)
                .totalPublicaciones(totalPublicaciones)
                .promedioPublicacionesPorProyecto(Math.round(promedio * 100.0) / 100.0)
                .build();
    }

    /**
     * 5. Índice de Participación Estudiantil en Autorías
     */
    public ParticipacionEstudiantilDTO calcularParticipacionEstudiantil() {
        List<PublicacionUsuario> autores = publicacionUsuarioRepository.findAll();
        Set<Integer> usuariosEstudiantes = estudianteRepository.findAll().stream()
                .map(e -> e.getUsuario().getId())
                .collect(Collectors.toSet());

        long totalAutores = autores.size();
        long totalAutoresEstudiantes = autores.stream()
                .filter(pu -> usuariosEstudiantes.contains(pu.getUsuario().getId())
                        || (pu.getRolAutor() != null && pu.getRolAutor().getNombre().toLowerCase().contains("estudiante")))
                .count();

        double porcentaje = totalAutores > 0 ? ((double) totalAutoresEstudiantes / totalAutores) * 100.0 : 0.0;

        return ParticipacionEstudiantilDTO.builder()
                .totalAutoresRegistrados(totalAutores)
                .totalAutoresEstudiantes(totalAutoresEstudiantes)
                .porcentajeParticipacionEstudiantil(Math.round(porcentaje * 100.0) / 100.0)
                .build();
    }

    /**
     * 6. Distribución del Impacto de Reconocimientos (por Entidad y Tipo)
     */
    public DistribucionImpactoReconocimientosDTO calcularDistribucionImpactoReconocimientos() {
        List<ReconocimientoSemillero> reconocimientos = reconocimientoRepository.findAll();

        Map<String, Long> porEntidad = reconocimientos.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getEntidadOtorgante() != null ? r.getEntidadOtorgante().getNombre() : "Sin Entidad",
                        Collectors.counting()
                ));

        Map<String, Long> porTipo = reconocimientos.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTipoReconocimiento() != null ? r.getTipoReconocimiento().getNombre() : "Sin Tipo",
                        Collectors.counting()
                ));

        return DistribucionImpactoReconocimientosDTO.builder()
                .totalReconocimientos(reconocimientos.size())
                .distribucionPorEntidad(porEntidad)
                .distribucionPorTipo(porTipo)
                .build();
    }

    /**
     * 7. Mapa de Habilidades Emergentes (Skill Trend en postulaciones aceptadas)
     */
    public List<HabilidadFrecuenciaDTO> calcularMapaHabilidadesEmergentes() {
        List<PostulacionHabilidad> postulacionHabilidades = postulacionHabilidadRepository.findAll();

        Map<String, Long> conteo = postulacionHabilidades.stream()
                .filter(ph -> ph.getPostulacion() != null
                        && ph.getPostulacion().getEstado() != null
                        && ph.getPostulacion().getEstado().getNombre().equalsIgnoreCase("Aceptada"))
                .collect(Collectors.groupingBy(
                        ph -> ph.getHabilidad().getNombre(),
                        Collectors.counting()
                ));

        return conteo.entrySet().stream()
                .map(entry -> HabilidadFrecuenciaDTO.builder()
                        .habilidad(entry.getKey())
                        .frecuencia(entry.getValue())
                        .build())
                .sorted(Comparator.comparingLong(HabilidadFrecuenciaDTO::getFrecuencia).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 8. Índice de Interdisciplinariedad: % de estudiantes cuyo id_programa difiere del programa del semillero / tutor
     */
    public InterdisciplinariedadDTO calcularIndiceInterdisciplinariedad() {
        List<EstudianteSemillero> vinculaciones = estudianteSemilleroRepository.findAll();

        if (vinculaciones.isEmpty()) {
            return InterdisciplinariedadDTO.builder()
                    .totalEstudiantesVinculados(0)
                    .estudiantesInterdisciplinarios(0L)
                    .porcentajeInterdisciplinariedad(0.0)
                    .build();
        }

        long interdisciplinarios = vinculaciones.stream()
                .filter(es -> {
                    if (es.getEstudiante() == null || es.getEstudiante().getPrograma() == null || es.getSemillero() == null || es.getSemillero().getPrograma() == null) {
                        return false;
                    }
                    return !es.getEstudiante().getPrograma().getId().equals(es.getSemillero().getPrograma().getId());
                })
                .count();

        double porcentaje = ((double) interdisciplinarios / vinculaciones.size()) * 100.0;

        return InterdisciplinariedadDTO.builder()
                .totalEstudiantesVinculados(vinculaciones.size())
                .estudiantesInterdisciplinarios(interdisciplinarios)
                .porcentajeInterdisciplinariedad(Math.round(porcentaje * 100.0) / 100.0)
                .build();
    }

    /**
     * Resumen completo de todos los KPIs institucionales para el Dashboard
     */
    public DashboardResumenDTO obtenerResumenDashboard() {
        return DashboardResumenDTO.builder()
                .tasasDemanda(calcularTasaDemandaPorSemillero())
                .tiempoRespuesta(calcularTiempoPromedioRespuesta())
                .tasaRetencion(calcularTasaRetencionRechazo())
                .conversionProyectos(calcularConversionProyectoPublicacion())
                .participacionEstudiantil(calcularParticipacionEstudiantil())
                .impactoReconocimientos(calcularDistribucionImpactoReconocimientos())
                .habilidadesEmergentes(calcularMapaHabilidadesEmergentes())
                .interdisciplinariedad(calcularIndiceInterdisciplinariedad())
                .build();
    }

    // ==========================================
    // DTOs DE ANALÍTICA
    // ==========================================

    @Data
    @Builder
    public static class TasaDemandaSemilleroDTO {
        private Integer idSemillero;
        private String nombreSemillero;
        private Integer cuposOfertados;
        private Long postulacionesRecibidas;
        private Double tasaDemanda;
    }

    @Data
    @Builder
    public static class TiempoPromedioRespuestaDTO {
        private Integer totalPostulacionesEvaluadas;
        private Double promedioHoras;
        private Double promedioDias;
    }

    @Data
    @Builder
    public static class TasaRetencionOfertasDTO {
        private Long ofertasAceptadas;
        private Long ofertasRechazadasPorEstudiante;
        private Long ofertasExpiradas;
        private Long totalOfertasProcesadas;
        private Double porcentajeRetencion;
        private Double porcentajeRechazoYExpiracion;
    }

    @Data
    @Builder
    public static class ConversionProyectoPublicacionDTO {
        private Long totalProyectos;
        private Long totalPublicaciones;
        private Double promedioPublicacionesPorProyecto;
    }

    @Data
    @Builder
    public static class ParticipacionEstudiantilDTO {
        private Long totalAutoresRegistrados;
        private Long totalAutoresEstudiantes;
        private Double porcentajeParticipacionEstudiantil;
    }

    @Data
    @Builder
    public static class DistribucionImpactoReconocimientosDTO {
        private Integer totalReconocimientos;
        private Map<String, Long> distribucionPorEntidad;
        private Map<String, Long> distribucionPorTipo;
    }

    @Data
    @Builder
    public static class HabilidadFrecuenciaDTO {
        private String habilidad;
        private Long frecuencia;
    }

    @Data
    @Builder
    public static class InterdisciplinariedadDTO {
        private Integer totalEstudiantesVinculados;
        private Long estudiantesInterdisciplinarios;
        private Double porcentajeInterdisciplinariedad;
    }

    @Data
    @Builder
    public static class DashboardResumenDTO {
        private List<TasaDemandaSemilleroDTO> tasasDemanda;
        private TiempoPromedioRespuestaDTO tiempoRespuesta;
        private TasaRetencionOfertasDTO tasaRetencion;
        private ConversionProyectoPublicacionDTO conversionProyectos;
        private ParticipacionEstudiantilDTO participacionEstudiantil;
        private DistribucionImpactoReconocimientosDTO impactoReconocimientos;
        private List<HabilidadFrecuenciaDTO> habilidadesEmergentes;
        private InterdisciplinariedadDTO interdisciplinariedad;
    }
}
