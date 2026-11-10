package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.dto.response.SemilleroRecomendadoDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteHabilidadRepository;
import com.udea.agora_backend.repository.EstudianteLineaInvestigacionRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.repository.ProfesorAreaEspecialidadRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.util.Redondeo;
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
 * Solo se recomiendan semilleros activos a los que el estudiante podría postularse:
 * se excluyen los que lidera y aquellos a los que ya pertenece.
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
    private final EstudianteHabilidadRepository estudianteHabilidadRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;

    // Pesos de los cuatro factores del puntaje (suman 1)
    private static final double PESO_HABILIDADES = 0.40;
    private static final double PESO_AREAS_TUTORES = 0.25;
    private static final double PESO_CONVOCATORIA_ABIERTA = 0.20;
    private static final double PESO_PROXIMIDAD_TEMATICA = 0.15;

    // Peso interno del Factor 4: coincidencia de líneas de investigación vs. mismo programa
    private static final double PESO_LINEAS_EN_FACTOR_4 = 0.75;
    private static final double PESO_PROGRAMA_EN_FACTOR_4 = 0.25;

    // Nombre del estado de postulación que indica selección confirmada. Debe coincidir
    // exactamente con el valor sembrado en tbl_estados (categoría "Estado Postulación").

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

        Set<Integer> semillerosDelEstudiante = estudianteSemilleroRepository.findByEstudianteIdAndFechaSalidaIsNull(idEstudiante)
                .stream()
                .map(es -> es.getSemillero().getId())
                .collect(Collectors.toSet());

        List<Semillero> semillerosDisponibles = semilleroRepository.findAll().stream()
                .filter(Semillero::estaActivo)
                .filter(s -> !s.getEstudianteLider().getId().equals(idEstudiante))
                .filter(s -> !semillerosDelEstudiante.contains(s.getId()))
                .toList();

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
        double porcentajeMatch = factor1 * PESO_HABILIDADES + factor2 * PESO_AREAS_TUTORES
                + factor3 * PESO_CONVOCATORIA_ABIERTA + factor4 * PESO_PROXIMIDAD_TEMATICA;
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
                .porcentajeMatch(Redondeo.dosDecimales(porcentajeMatch))
                .factor1Habilidades(Redondeo.dosDecimales(factor1))
                .factor2AreaEspecialidad(Redondeo.dosDecimales(factor2))
                .factor3DisponibilidadConvocatorias(Redondeo.dosDecimales(factor3))
                .factor4ProximidadTematica(Redondeo.dosDecimales(factor4))
                .build();
    }

    /**
     * FACTOR 1 (40%): Calcula coincidencia de habilidades
     * Compara habilidades del estudiante con las que históricamente ha requerido el semillero.
     * Las del estudiante son las de su perfil (tbl_estudiante_x_habilidad, desde el registro)
     * más las que declaró en sus postulaciones.
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
        Set<Integer> idsRequeridos = postulacionHabilidadRepository
                .findBySemilleroIdAndEstadoNombre(semillero.getId(), EstadosPostulacion.ACEPTADA)
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

        // Habilidades declaradas en el perfil (desde el registro) más las que ha declarado
        // en sus postulaciones
        Set<Integer> idsEstudiante = new HashSet<>(
                estudianteHabilidadRepository.findHabilidadIdsByEstudianteId(estudiante.getId())
        );
        idsEstudiante.addAll(postulacionHabilidadRepository.findHabilidadIdsByEstudianteId(estudiante.getId()));

        if (idsEstudiante.isEmpty()) {
            // Perfil anterior a la migración V3 sin habilidades ni postulaciones: no hay con
            // qué comparar, así que no se le penaliza (mismo criterio que un semillero sin historial)
            return FACTOR_NEUTRAL_SIN_HISTORIAL;
        }

        Set<Integer> interseccion = new HashSet<>(idsEstudiante);
        interseccion.retainAll(idsRequeridos);

        double coincidencia = (double) interseccion.size() / idsRequeridos.size();
        return Math.min(100.0, coincidencia * 100.0);
    }

    /**
     * FACTOR 2 (25%): Calcula afinidad en áreas de especialidad
     * Compara las áreas de especialidad del estudiante (derivadas de sus líneas de
     * investigación de interés, vía la nueva FK tbl_linea_investigacion.id_area_especialidad)
     * contra las áreas de especialidad declaradas por TODOS los profesores tutores del
     * semillero (co-tutoría N:M vía tbl_profesor_x_semillero + tbl_profesor_x_area_especialidad).
     */
    private double calcularFactorAreaEspecialidad(Estudiante estudiante, Semillero semillero) {
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
    }

    /**
     * FACTOR 3 (20%): Calcula disponibilidad de convocatorias
     * Verifica si el semillero tiene convocatorias abiertas, vigentes hoy y con cupos
     */
    private double calcularFactorDisponibilidadConvocatorias(Semillero semillero) {
        // Con una convocatoria que hoy recibe postulaciones: 100 %; si no: 0 %
        return convocatoriaRepository.existeConvocatoriaRecibiendoPostulaciones(semillero.getId(), LocalDate.now())
                ? 100.0 : 0.0;
    }

    /**
     * FACTOR 4 (15%): Calcula proximidad temática y de programa
     * 75%: coincidencia en líneas de investigación entre estudiante y semillero.
     * 25%: el estudiante pertenece al mismo programa académico del semillero.
     */
    private double calcularFactorProximidadTematica(Estudiante estudiante, Semillero semillero) {
        // Obtener líneas de interés del estudiante
        Set<Integer> lineasEstudiante = estudianteLineaRepository.findByEstudianteId(estudiante.getId())
                .stream()
                .map(eli -> eli.getLineaInvestigacion().getId())
                .collect(Collectors.toSet());

        double programa = estudiante.getPrograma() != null && semillero.getPrograma() != null
                && estudiante.getPrograma().getId().equals(semillero.getPrograma().getId()) ? 100.0 : 0.0;

        if (lineasEstudiante.isEmpty()) {
            return programa * PESO_PROGRAMA_EN_FACTOR_4; // Estudiante sin líneas de interés
        }

        // Obtener líneas del semillero
        Set<Integer> lineasSemillero = semilleroLineaRepository.findBySemilleroId(semillero.getId())
                .stream()
                .map(sli -> sli.getLineaInvestigacion().getId())
                .collect(Collectors.toSet());

        if (lineasSemillero.isEmpty()) {
            return programa * PESO_PROGRAMA_EN_FACTOR_4; // Semillero sin líneas definidas
        }

        // Calcular coincidencia
        Set<Integer> interseccion = new HashSet<>(lineasEstudiante);
        interseccion.retainAll(lineasSemillero);

        double lineas = Math.min(100.0, (double) interseccion.size() / lineasSemillero.size() * 100.0);
        return lineas * PESO_LINEAS_EN_FACTOR_4 + programa * PESO_PROGRAMA_EN_FACTOR_4;
    }

}
