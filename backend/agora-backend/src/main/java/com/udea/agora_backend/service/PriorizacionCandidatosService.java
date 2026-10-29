package com.udea.agora_backend.service;

import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.repository.EstudianteLineaInvestigacionRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SERVICIO 2 (complemento del Gestor de Convocatorias): Priorización de candidatos.
 *
 * Para cada convocatoria de un semillero ordena a los aspirantes que siguen en proceso
 * (Pendiente o Pre-aprobada) con un puntaje de 0 a 100, para que el líder decida a quién
 * ofrecer los cupos. Ponderación:
 *   - Afinidad temática (35%): líneas de investigación del estudiante que coinciden con las del semillero.
 *   - Habilidades (25%): habilidades declaradas en la postulación que el semillero ya usa
 *     (las de sus postulaciones aceptadas). Sin ese historial, valor neutral.
 *   - Disponibilidad (20%): horas semanales declaradas; 12 h o más es el máximo.
 *   - Compromiso (20%): cómo respondió el estudiante a ofertas anteriores
 *     (aceptó 100, aceptó otra 75, declinó 50, la dejó vencer 0). Sin historial, valor neutral.
 * Los empates se resuelven por orden de llegada.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriorizacionCandidatosService {

    private static final double PESO_TEMATICA = 0.35;
    private static final double PESO_HABILIDADES = 0.25;
    private static final double PESO_DISPONIBILIDAD = 0.20;
    private static final double PESO_COMPROMISO = 0.20;
    private static final double VALOR_NEUTRAL = 50.0;
    private static final int HORAS_MAXIMO_PUNTAJE = 12;

    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_PRE_APROBADA = "Pre-aprobada";
    private static final String ESTADO_ACEPTADA = "Aceptada";
    private static final String ESTADO_RECHAZADA_ESTUDIANTE = "Rechazada por Estudiante";
    private static final String ESTADO_CANCELADA = "Cancelada en cascada";
    private static final String ESTADO_EXPIRADA = "Expirada";

    private final PostulacionRepository postulacionRepository;
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final PlazoOfertasService plazoOfertasService;

    public List<CandidatoPriorizadoDTO> priorizarPorSemillero(Integer idSemillero) {
        Set<Integer> lineasSemillero = semilleroLineaRepository.findBySemilleroId(idSemillero).stream()
                .map(sl -> sl.getLineaInvestigacion().getId())
                .collect(Collectors.toSet());
        Set<Integer> habilidadesSemillero = postulacionHabilidadRepository
                .findBySemilleroIdAndEstadoNombre(idSemillero, ESTADO_ACEPTADA).stream()
                .map(ph -> ph.getHabilidad().getId())
                .collect(Collectors.toSet());

        Map<Integer, List<Postulacion>> enProcesoPorConvocatoria = postulacionRepository
                .findByConvocatoriaSemilleroId(idSemillero).stream()
                .filter(p -> tieneEstado(p, ESTADO_PENDIENTE) || tieneEstado(p, ESTADO_PRE_APROBADA))
                .collect(Collectors.groupingBy(p -> p.getConvocatoria().getId(), LinkedHashMap::new, Collectors.toList()));

        ZonedDateTime ahora = ZonedDateTime.now();
        List<CandidatoPriorizadoDTO> resultado = new ArrayList<>();
        enProcesoPorConvocatoria.values().forEach(postulaciones -> {
            Convocatoria convocatoria = postulaciones.get(0).getConvocatoria();
            long ofertasVigentes = postulacionRepository.contarOfertasVigentes(
                    convocatoria.getId(), ESTADO_PRE_APROBADA, plazoOfertasService.inicioOfertasVigentes(ahora));

            List<CandidatoPriorizadoDTO> candidatos = postulaciones.stream()
                    .map(p -> evaluar(p, lineasSemillero, habilidadesSemillero, idSemillero, convocatoria, ofertasVigentes))
                    .sorted(Comparator.comparingDouble(CandidatoPriorizadoDTO::getPuntaje).reversed()
                            .thenComparing(CandidatoPriorizadoDTO::getFechaPostulacion))
                    .toList();
            for (int i = 0; i < candidatos.size(); i++) {
                candidatos.get(i).setPosicion(i + 1);
            }
            resultado.addAll(candidatos);
        });
        return resultado;
    }

    private CandidatoPriorizadoDTO evaluar(Postulacion postulacion, Set<Integer> lineasSemillero,
                                           Set<Integer> habilidadesSemillero, Integer idSemillero,
                                           Convocatoria convocatoria, long ofertasVigentes) {
        Integer idEstudiante = postulacion.getEstudiante().getId();
        List<String> observaciones = new ArrayList<>();

        double tematica = calcularAfinidadTematica(idEstudiante, lineasSemillero);
        double habilidades = calcularHabilidades(postulacion, habilidadesSemillero);
        double disponibilidad = Math.min(1.0,
                (double) postulacion.getDisponibilidadHorasSemana() / HORAS_MAXIMO_PUNTAJE) * 100.0;

        List<Postulacion> otras = postulacionRepository.findByEstudianteId(idEstudiante).stream()
                .filter(p -> !p.getId().equals(postulacion.getId()))
                .toList();
        double compromiso = calcularCompromiso(otras);
        if (otras.stream().noneMatch(this::recibioOfertaResuelta)) {
            observaciones.add("Sin historial de ofertas anteriores");
        }
        otras.stream()
                .filter(p -> tieneEstado(p, ESTADO_PRE_APROBADA))
                .forEach(p -> observaciones.add("Tiene otra oferta vigente en "
                        + p.getConvocatoria().getSemillero().getNombre()));
        long otrosSemilleros = estudianteSemilleroRepository.findByEstudianteIdAndFechaSalidaIsNull(idEstudiante).stream()
                .filter(es -> !es.getSemillero().getId().equals(idSemillero))
                .count();
        if (otrosSemilleros > 0) {
            observaciones.add("Ya integra " + otrosSemilleros + " semillero(s)");
        }

        double puntaje = tematica * PESO_TEMATICA + habilidades * PESO_HABILIDADES
                + disponibilidad * PESO_DISPONIBILIDAD + compromiso * PESO_COMPROMISO;

        return CandidatoPriorizadoDTO.builder()
                .idPostulacion(postulacion.getId())
                .idConvocatoria(convocatoria.getId())
                .tituloConvocatoria(convocatoria.getTitulo())
                .cuposDisponibles(convocatoria.getCuposDisponibles())
                .ofertasVigentes(ofertasVigentes)
                .fechaPostulacion(postulacion.getFechaPostulacion())
                .puntaje(redondear(puntaje))
                .afinidadTematica(redondear(tematica))
                .habilidades(redondear(habilidades))
                .disponibilidad(redondear(disponibilidad))
                .compromiso(redondear(compromiso))
                .observaciones(observaciones)
                .build();
    }

    private double calcularAfinidadTematica(Integer idEstudiante, Set<Integer> lineasSemillero) {
        if (lineasSemillero.isEmpty()) {
            return VALOR_NEUTRAL;
        }
        Set<Integer> lineasEstudiante = estudianteLineaRepository.findByEstudianteId(idEstudiante).stream()
                .map(el -> el.getLineaInvestigacion().getId())
                .collect(Collectors.toCollection(HashSet::new));
        lineasEstudiante.retainAll(lineasSemillero);
        return (double) lineasEstudiante.size() / lineasSemillero.size() * 100.0;
    }

    // Qué parte de lo que el aspirante declara ya lo usa el semillero
    private double calcularHabilidades(Postulacion postulacion, Set<Integer> habilidadesSemillero) {
        Set<Integer> declaradas = postulacionHabilidadRepository.findByPostulacionId(postulacion.getId()).stream()
                .map(ph -> ph.getHabilidad().getId())
                .collect(Collectors.toCollection(HashSet::new));
        if (habilidadesSemillero.isEmpty() || declaradas.isEmpty()) {
            return VALOR_NEUTRAL;
        }
        int total = declaradas.size();
        declaradas.retainAll(habilidadesSemillero);
        return (double) declaradas.size() / total * 100.0;
    }

    private double calcularCompromiso(List<Postulacion> otras) {
        List<Double> respuestas = otras.stream()
                .filter(this::recibioOfertaResuelta)
                .map(p -> {
                    if (tieneEstado(p, ESTADO_ACEPTADA)) return 100.0;
                    if (tieneEstado(p, ESTADO_CANCELADA)) return 75.0;
                    if (tieneEstado(p, ESTADO_RECHAZADA_ESTUDIANTE)) return 50.0;
                    return 0.0; // Expirada con oferta: no respondió
                })
                .toList();
        return respuestas.isEmpty()
                ? VALOR_NEUTRAL
                : respuestas.stream().mapToDouble(Double::doubleValue).average().orElse(VALOR_NEUTRAL);
    }

    // Ofertas reales ya resueltas: mismo criterio que la retención de ofertas de la analítica
    private boolean recibioOfertaResuelta(Postulacion p) {
        return p.getFechaPreAprobacion() != null && (tieneEstado(p, ESTADO_ACEPTADA) || tieneEstado(p, ESTADO_RECHAZADA_ESTUDIANTE)
                || tieneEstado(p, ESTADO_CANCELADA) || tieneEstado(p, ESTADO_EXPIRADA));
    }

    private static boolean tieneEstado(Postulacion postulacion, String nombre) {
        return postulacion.getEstado() != null && postulacion.getEstado().getNombre().equalsIgnoreCase(nombre);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    @Data
    @Builder
    public static class CandidatoPriorizadoDTO {
        private Integer idPostulacion;
        private Integer idConvocatoria;
        private String tituloConvocatoria;
        // Posición dentro de su convocatoria (1 = mejor candidato)
        private Integer posicion;
        private Integer cuposDisponibles;
        private Long ofertasVigentes;
        private LocalDate fechaPostulacion;
        private Double puntaje;
        private Double afinidadTematica;
        private Double habilidades;
        private Double disponibilidad;
        private Double compromiso;
        private List<String> observaciones;
    }
}
