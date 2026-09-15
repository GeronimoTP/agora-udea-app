package com.udea.agora_backend.service;

import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SERVICIO CRÍTICO: Motor de Recomendación de Semilleros
 * 
 * Implementa algoritmo de puntuación ponderada con 4 factores:
 * - Factor 1: Habilidades Coincidentes (40%)
 * - Factor 2: Afinidad Áreas de Especialidad (25%)
 * - Factor 3: Disponibilidad Convocatorias (20%)
 * - Factor 4: Proximidad Temática y Programa (15%)
 * 
 * Retorna ranking ordenado por porcentaje de afinidad (0-100%)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MotorRecomendacionService {

    private final EstudianteRepository estudianteRepository;
    private final SemilleroRepository semilleroRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final ProfesorAreaEspecialidadRepository profesorAreaRepository;
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;

    // Nombre del estado de postulación que indica selección confirmada. Debe coincidir
    // exactamente con el valor sembrado en tbl_estados (categoría "Estado Postulación").
    private static final String ESTADO_POSTULACION_ACEPTADA = "Aceptada";

    // Valor neutral (ni penaliza ni favorece) para el Factor 1 cuando un semillero no
    // tiene ningún historial de postulaciones con el que evaluar el fit técnico.
    private static final double FACTOR_NEUTRAL_SIN_HISTORIAL = 50.0;

    /**
     * Calcula recomendaciones de semilleros para un estudiante específico
     * @param idEstudiante ID del estudiante
     * @return Lista de semilleros recomendados ordenados por afinidad (desc)
     */
    public List<SemilleroRecomendadoDTO> calcularAfinidad(Integer idEstudiante) {
        log.info("Iniciando cálculo de afinidad para estudiante ID: {}", idEstudiante);
        
        // Verificar que el estudiante existe
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", idEstudiante));

        // Obtener todos los semilleros disponibles
        List<Semillero> semillerosDisponibles = semilleroRepository.findAll();
        
        if (semillerosDisponibles.isEmpty()) {
            log.warn("No hay semilleros disponibles para recomendar");
            return new ArrayList<>();
        }

        // Calcular afinidad para cada semillero
        List<SemilleroRecomendadoDTO> recomendaciones = semillerosDisponibles.stream()
                .map(semillero -> calcularAfinidadConSemillero(estudiante, semillero))
                .sorted(Comparator.comparingDouble(SemilleroRecomendadoDTO::getPorcentajeMatch).reversed())
                .collect(Collectors.toList());

        log.info("Cálculo completado. {} semilleros recomendados", recomendaciones.size());
        return recomendaciones;
    }

    /**
     * Calcula la afinidad entre un estudiante y un semillero específico
     */
    private SemilleroRecomendadoDTO calcularAfinidadConSemillero(Estudiante estudiante, Semillero semillero) {
        log.debug("Calculando afinidad: Estudiante {} - Semillero {}", estudiante.getId(), semillero.getId());

        // Calcular los 4 factores
        double factor1 = calcularFactorHabilidades(estudiante, semillero);
        double factor2 = calcularFactorAreaEspecialidad(estudiante, semillero);
        double factor3 = calcularFactorDisponibilidadConvocatorias(semillero);
        double factor4 = calcularFactorProximidadTematica(estudiante, semillero);

        // Aplicar pesos
        double porcentajeMatch = (factor1 * 0.40) + (factor2 * 0.25) + (factor3 * 0.20) + (factor4 * 0.15);
        porcentajeMatch = Math.min(100.0, Math.max(0.0, porcentajeMatch)); // Limitar 0-100%

        log.debug("Factores calculados - Habilidades: {}, AreaEsp: {}, Cupos: {}, Temática: {}, Total: {}%",
                factor1, factor2, factor3, factor4, porcentajeMatch);

        return SemilleroRecomendadoDTO.builder()
                .idSemillero(semillero.getId())
                .nombreSemillero(semillero.getNombre())
                .codigoIdentificador(semillero.getCodigoIdentificador())
                .descripcion(semillero.getDescripcion())
                .nombreEstudianteLider(semillero.getEstudianteLider().getUsuario().getNombreCompleto())
                .nombrePrograma(semillero.getPrograma().getNombre())
                .porcentajeMatch(Math.round(porcentajeMatch * 100.0) / 100.0) // Redondear a 2 decimales
                .factor1Habilidades(Math.round(factor1 * 100.0) / 100.0)
                .factor2AreaEspecialidad(Math.round(factor2 * 100.0) / 100.0)
                .factor3DisponibilidadConvocatorias(Math.round(factor3 * 100.0) / 100.0)
                .factor4ProximidadTematica(Math.round(factor4 * 100.0) / 100.0)
                .build();
    }

/**
     * FACTOR 1 (40%): Calcula coincidencia de habilidades
     * Compara habilidades del estudiante con las que históricamente ha requerido el semillero.
     *
     * Estrategia de tres niveles (ajuste de código, sin cambios de esquema):
     *   1. Señal fuerte: habilidades de postulaciones ya ACEPTADAS al semillero (lo que
     *      realmente funcionó y fue seleccionado).
     *   2. Fallback: si aún no hay postulaciones aceptadas (semillero nuevo o convocatoria
     *      recién abierta), se usan las habilidades de TODAS las postulaciones recibidas.
     *   3. Neutral: si el semillero nunca ha recibido ninguna postulación, no hay forma de
     *      evaluar el fit técnico, así que se devuelve un valor neutral (no 0, que penalizaría
     *      injustamente a semilleros nuevos sin culpa del estudiante).
     */
    private double calcularFactorHabilidades(Estudiante estudiante, Semillero semillero) {
        try {
            Set<Integer> idsRequeridos = postulacionHabilidadRepository
                    .findBySemilleroIdAndEstadoNombre(semillero.getId(), ESTADO_POSTULACION_ACEPTADA)
                    .stream()
                    .map(ph -> ph.getHabilidad().getId())
                    .collect(Collectors.toSet());

            if (idsRequeridos.isEmpty()) {
                idsRequeridos = postulacionHabilidadRepository.findBySemilleroId(semillero.getId())
                        .stream()
                        .map(ph -> ph.getHabilidad().getId())
                        .collect(Collectors.toSet());
            }

            if (idsRequeridos.isEmpty()) {
                return FACTOR_NEUTRAL_SIN_HISTORIAL; // Semillero sin ninguna postulación aún: sin señal
            }

            Set<Integer> idsEstudiante = new HashSet<>(
                    postulacionHabilidadRepository.findHabilidadIdsByEstudianteId(estudiante.getId())
            );

            if (idsEstudiante.isEmpty()) {
                return 0.0; // Sí hay señal del semillero, pero el estudiante no declaró habilidades
            }

            Set<Integer> interseccion = new HashSet<>(idsEstudiante);
            interseccion.retainAll(idsRequeridos);

            double coincidencia = (double) interseccion.size() / idsRequeridos.size();
            return Math.min(100.0, coincidencia * 100.0);
        } catch (Exception e) {
            log.error("Error calculando Factor 1 (Habilidades)", e);
            return 0.0;
        }
    }

    /**
     * FACTOR 2 (25%): Calcula afinidad en áreas de especialidad
     * Compara las áreas de especialidad del estudiante (derivadas de sus líneas de
     * investigación de interés, vía la nueva FK tbl_linea_investigacion.id_area_especialidad)
     * contra las áreas de especialidad declaradas por TODOS los profesores tutores del
     * semillero (co-tutoría N:M vía tbl_profesor_x_semillero + tbl_profesor_x_area_especialidad).
     */
    private double calcularFactorAreaEspecialidad(Estudiante estudiante, Semillero semillero) {
        try {
            Set<Integer> areasEstudiante = estudianteLineaRepository.findByEstudianteId(estudiante.getId())
                    .stream()
                    .map(eli -> eli.getLineaInvestigacion().getAreaEspecialidad().getId())
                    .collect(Collectors.toSet());

            if (areasEstudiante.isEmpty()) {
                return 0.0; // Estudiante sin líneas de interés registradas
            }

            Set<Integer> areasProfesores = profesorSemilleroRepository.findBySemilleroId(semillero.getId())
                    .stream()
                    .flatMap(ps -> profesorAreaRepository.findByProfesorId(ps.getProfesor().getId()).stream())
                    .map(pae -> pae.getAreaEspecialidad().getId())
                    .collect(Collectors.toSet());

            if (areasProfesores.isEmpty()) {
                return 0.0; // El semillero aún no tiene profesores tutores con áreas registradas
            }

            Set<Integer> interseccion = new HashSet<>(areasEstudiante);
            interseccion.retainAll(areasProfesores);

            double coincidencia = (double) interseccion.size() / areasProfesores.size();
            return Math.min(100.0, coincidencia * 100.0);
        } catch (Exception e) {
            log.error("Error calculando Factor 2 (Área Especialidad)", e);
            return 0.0;
        }
    }

    /**
     * FACTOR 3 (20%): Calcula disponibilidad de convocatorias
     * Verifica si el semillero tiene convocatorias activas con cupos
     */
    private double calcularFactorDisponibilidadConvocatorias(Semillero semillero) {
        try {
            LocalDate hoy = LocalDate.now();
            
            long convocatoriasActivas = convocatoriaRepository.findBySemilleroId(semillero.getId())
                    .stream()
                    .filter(conv -> !conv.getFechaCierre().isBefore(hoy)) // No expirada
                    .filter(conv -> conv.getCuposDisponibles() > 0) // Con cupos
                    .count();

            // Si hay convocatorias activas: 100%, si no: 0%
            return convocatoriasActivas > 0 ? 100.0 : 0.0;
        } catch (Exception e) {
            log.error("Error calculando Factor 3 (Disponibilidad Convocatorias)", e);
            return 0.0;
        }
    }

    /**
     * FACTOR 4 (15%): Calcula proximidad temática
     * Mide coincidencia en líneas de investigación entre estudiante y semillero
     */
    private double calcularFactorProximidadTematica(Estudiante estudiante, Semillero semillero) {
        try {
            // Obtener líneas de interés del estudiante
            Set<Integer> lineasEstudiante = estudianteLineaRepository.findByEstudianteId(estudiante.getId())
                    .stream()
                    .map(eli -> eli.getLineaInvestigacion().getId())
                    .collect(Collectors.toSet());

            if (lineasEstudiante.isEmpty()) {
                return 0.0; // Estudiante sin líneas de interés
            }

            // Obtener líneas del semillero
            Set<Integer> lineasSemillero = semilleroLineaRepository.findBySemilleroId(semillero.getId())
                    .stream()
                    .map(sli -> sli.getLineaInvestigacion().getId())
                    .collect(Collectors.toSet());

            if (lineasSemillero.isEmpty()) {
                return 0.0; // Semillero sin líneas definidas
            }

            // Calcular coincidencia
            Set<Integer> interseccion = new HashSet<>(lineasEstudiante);
            interseccion.retainAll(lineasSemillero);

            double coincidencia = (double) interseccion.size() / lineasSemillero.size();
            return Math.min(100.0, coincidencia * 100.0);
        } catch (Exception e) {
            log.error("Error calculando Factor 4 (Proximidad Temática)", e);
            return 0.0;
        }
    }

    /**
     * DTO para respuesta del motor de recomendación
     */
    @lombok.Data
    @lombok.Builder
    public static class SemilleroRecomendadoDTO {
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
}
