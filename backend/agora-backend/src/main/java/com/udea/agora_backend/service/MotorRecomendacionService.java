package com.udea.agora_backend.service;

import com.udea.agora_backend.config.PesosRecomendacion;
import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.dto.response.SemilleroRecomendadoDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteLineaInvestigacion;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteHabilidadRepository;
import com.udea.agora_backend.repository.EstudianteLineaInvestigacionRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository.HabilidadDeSemillero;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository.AreaDeTutor;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository.LineaDeSemillero;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.util.Redondeo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SERVICIO CRÍTICO: Motor de Recomendación de Semilleros
 *
 * Implementa algoritmo de puntuación ponderada con 4 factores (pesos por defecto, configurables
 * en {@link PesosRecomendacion}):
 * - Factor 1: Habilidades Coincidentes (40%)
 * - Factor 2: Afinidad Áreas de Especialidad (25%)
 * - Factor 3: Disponibilidad Convocatorias (20%)
 * - Factor 4: Proximidad Temática y Programa (15%)
 *
 * Solo se recomiendan semilleros activos a los que el estudiante podría postularse:
 * se excluyen los que lidera y aquellos a los que ya pertenece.
 * Retorna ranking ordenado por porcentaje de afinidad (0-100%)
 *
 * <p>Los datos se cargan antes de puntuar: el perfil del estudiante una sola vez y los de todos
 * los semilleros candidatos en cuatro consultas agrupadas ({@link DatosSemilleros}). El número
 * de consultas no crece con la cantidad de semilleros.
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
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final EstudianteHabilidadRepository estudianteHabilidadRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final PesosRecomendacion pesos;

    // Valor neutral (ni penaliza ni favorece) para el Factor 1 cuando un semillero no
    // tiene ningún historial de postulaciones con el que evaluar el fit técnico.
    private static final double FACTOR_NEUTRAL_SIN_HISTORIAL = 50.0;

    /** Lo que se compara del estudiante, igual para todos los semilleros. */
    private record PerfilEstudiante(Set<Integer> habilidades, Set<Integer> lineas, Set<Integer> areas) {
    }

    /** Datos de los semilleros candidatos, agrupados por ID de semillero. */
    private record DatosSemilleros(Map<Integer, Set<Integer>> habilidadesAceptadas,
                                   Map<Integer, Set<Integer>> habilidadesRecibidas,
                                   Map<Integer, Set<Integer>> areasTutores,
                                   Map<Integer, Set<Integer>> lineas,
                                   Set<Integer> conConvocatoriaAbierta) {
    }

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

        PerfilEstudiante perfil = cargarPerfil(idEstudiante);
        DatosSemilleros datos = cargarDatosSemilleros(semillerosDisponibles.stream().map(Semillero::getId).toList());

        // Calcular afinidad para cada semillero
        List<SemilleroRecomendadoDTO> recomendaciones = semillerosDisponibles.stream()
                .map(semillero -> calcularAfinidadConSemillero(estudiante, perfil, semillero, datos))
                .sorted(Comparator.comparingDouble(SemilleroRecomendadoDTO::getPorcentajeMatch).reversed())
                .collect(Collectors.toList());

        log.info("Cálculo completado. {} semilleros recomendados", recomendaciones.size());
        return recomendaciones;
    }

    // ==========================================
    // CARGA DE DATOS
    // ==========================================

    /**
     * Habilidades del perfil (tbl_estudiante_x_habilidad, desde el registro) más las declaradas
     * en sus postulaciones; líneas de interés y el área de especialidad de cada una.
     */
    private PerfilEstudiante cargarPerfil(Integer idEstudiante) {
        Set<Integer> habilidades = new HashSet<>(estudianteHabilidadRepository.findHabilidadIdsByEstudianteId(idEstudiante));
        habilidades.addAll(postulacionHabilidadRepository.findHabilidadIdsByEstudianteId(idEstudiante));

        List<EstudianteLineaInvestigacion> lineasInteres = estudianteLineaRepository.findByEstudianteIdIn(List.of(idEstudiante));
        Set<Integer> lineas = lineasInteres.stream()
                .map(el -> el.getLineaInvestigacion().getId())
                .collect(Collectors.toSet());
        Set<Integer> areas = lineasInteres.stream()
                .map(el -> el.getLineaInvestigacion().getAreaEspecialidad().getId())
                .collect(Collectors.toSet());
        return new PerfilEstudiante(habilidades, lineas, areas);
    }

    private DatosSemilleros cargarDatosSemilleros(List<Integer> ids) {
        List<HabilidadDeSemillero> historial = postulacionHabilidadRepository.findHabilidadesDeSemilleros(ids);
        return new DatosSemilleros(
                agrupar(historial.stream().filter(h -> EstadosPostulacion.ACEPTADA.equals(h.getEstadoPostulacion())).toList(),
                        HabilidadDeSemillero::getIdSemillero, HabilidadDeSemillero::getIdHabilidad),
                agrupar(historial, HabilidadDeSemillero::getIdSemillero, HabilidadDeSemillero::getIdHabilidad),
                agrupar(profesorSemilleroRepository.findAreasDeTutores(ids), AreaDeTutor::getIdSemillero, AreaDeTutor::getIdArea),
                agrupar(semilleroLineaRepository.findLineasDeSemilleros(ids),
                        LineaDeSemillero::getIdSemillero, LineaDeSemillero::getIdLinea),
                convocatoriaRepository.idsSemillerosRecibiendoPostulaciones(ids, LocalDate.now()));
    }

    private static <T> Map<Integer, Set<Integer>> agrupar(List<T> filas, Function<T, Integer> semillero,
                                                          Function<T, Integer> valor) {
        return filas.stream().collect(Collectors.groupingBy(semillero, Collectors.mapping(valor, Collectors.toSet())));
    }

    // ==========================================
    // PUNTUACIÓN
    // ==========================================

    /**
     * Calcula la afinidad entre un estudiante y un semillero específico
     */
    private SemilleroRecomendadoDTO calcularAfinidadConSemillero(Estudiante estudiante, PerfilEstudiante perfil,
                                                                 Semillero semillero, DatosSemilleros datos) {
        log.debug("Calculando afinidad: Estudiante {} - Semillero {}", estudiante.getId(), semillero.getId());
        Integer id = semillero.getId();

        // Calcular los 4 factores
        double factor1 = calcularFactorHabilidades(perfil.habilidades(),
                datos.habilidadesAceptadas().getOrDefault(id, Set.of()),
                datos.habilidadesRecibidas().getOrDefault(id, Set.of()));
        double factor2 = calcularFactorAreaEspecialidad(perfil.areas(), datos.areasTutores().getOrDefault(id, Set.of()));
        double factor3 = datos.conConvocatoriaAbierta().contains(id) ? 100.0 : 0.0;
        double factor4 = calcularFactorProximidadTematica(estudiante, perfil.lineas(), semillero,
                datos.lineas().getOrDefault(id, Set.of()));

        // Aplicar pesos
        double porcentajeMatch = factor1 * pesos.habilidades() + factor2 * pesos.areasTutores()
                + factor3 * pesos.convocatoriaAbierta() + factor4 * pesos.proximidadTematica();
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
    private double calcularFactorHabilidades(Set<Integer> idsEstudiante, Set<Integer> aceptadas,
                                             Set<Integer> recibidas) {
        Set<Integer> idsRequeridos = aceptadas.isEmpty() ? recibidas : aceptadas;

        if (idsRequeridos.isEmpty()) {
            return FACTOR_NEUTRAL_SIN_HISTORIAL; // Semillero sin ninguna postulación aún: sin señal
        }

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
    private double calcularFactorAreaEspecialidad(Set<Integer> areasEstudiante, Set<Integer> areasProfesores) {
        if (areasEstudiante.isEmpty()) {
            return 0.0; // Estudiante sin líneas de interés registradas
        }

        if (areasProfesores.isEmpty()) {
            return 0.0; // El semillero aún no tiene profesores tutores con áreas registradas
        }

        Set<Integer> interseccion = new HashSet<>(areasEstudiante);
        interseccion.retainAll(areasProfesores);

        double coincidencia = (double) interseccion.size() / areasProfesores.size();
        return Math.min(100.0, coincidencia * 100.0);
    }

    // FACTOR 3 (20%): 100 % si el semillero tiene hoy una convocatoria abierta, iniciada,
    // vigente y con cupos; si no, 0 %. Se resuelve en cargarDatosSemilleros.

    /**
     * FACTOR 4 (15%): Calcula proximidad temática y de programa
     * 75%: coincidencia en líneas de investigación entre estudiante y semillero.
     * 25%: el estudiante pertenece al mismo programa académico del semillero.
     */
    private double calcularFactorProximidadTematica(Estudiante estudiante, Set<Integer> lineasEstudiante,
                                                    Semillero semillero, Set<Integer> lineasSemillero) {
        double programa = estudiante.getPrograma() != null && semillero.getPrograma() != null
                && estudiante.getPrograma().getId().equals(semillero.getPrograma().getId()) ? 100.0 : 0.0;

        if (lineasEstudiante.isEmpty()) {
            return programa * pesos.programaEnProximidad(); // Estudiante sin líneas de interés
        }

        if (lineasSemillero.isEmpty()) {
            return programa * pesos.programaEnProximidad(); // Semillero sin líneas definidas
        }

        // Calcular coincidencia
        Set<Integer> interseccion = new HashSet<>(lineasEstudiante);
        interseccion.retainAll(lineasSemillero);

        double lineas = Math.min(100.0, (double) interseccion.size() / lineasSemillero.size() * 100.0);
        return lineas * pesos.lineasEnProximidad() + programa * pesos.programaEnProximidad();
    }

}
