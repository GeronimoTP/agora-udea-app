package com.udea.agora_backend.service;

import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * SERVICIO CRÍTICO: Gestor de Convocatorias
 *
 * Implementa la lógica compleja de gestión de convocatorias, incluyendo el
 * Flujo de Selección de Doble Vía:
 *
 *   Pendiente --(líder pre-aprueba)--> Pre-aprobada --(estudiante confirma)--> Aceptada
 *      |                                    |
 *      +--(líder rechaza)--> Rechazada      +--(estudiante rechaza oferta)--> Rechazada por Estudiante
 *
 * 1. Postulación Concurrente: Permite múltiples postulaciones activas por estudiante
 * 2. Doble Vía: el líder pre-aprueba (filtra candidatos) y el estudiante decide
 * 3. Cascada de Rechazos: al confirmar una aceptación, libera las demás ofertas/postulaciones activas
 * 4. Gestión Dinámica de Cupos: decrementa cupos solo con la confirmación del estudiante
 * 5. Cierre Automático: marca la convocatoria como cerrada cuando los cupos llegan a 0
 * 6. Expiración de Ofertas: cron diario que cierra convocatorias vencidas y expira lo que quedó sin resolver
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GestorConvocatoriasService {

    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_PRE_APROBADA = "Pre-aprobada";
    private static final String ESTADO_ACEPTADA = "Aceptada";
    private static final String ESTADO_RECHAZADA = "Rechazada";
    private static final String ESTADO_RECHAZADA_ESTUDIANTE = "Rechazada por Estudiante";
    private static final String ESTADO_CERRADA = "Cerrada";
    private static final String ESTADO_EXPIRADA = "Expirada";

    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EstadoRepository estadoRepository;

    /**
     * PASO 1 DEL FLUJO DOBLE VÍA (acción del líder): pre-aprueba una postulación.
     * No mueve cupos ni dispara cascada; solo indica que el líder filtró y quiere
     * ofrecerle el cupo al estudiante. La decisión final queda en manos del estudiante.
     */
    public void preAprobarPostulacion(Integer idPostulacion) {
        log.info("Pre-aprobando postulación: {}", idPostulacion);

        Postulacion postulacion = obtenerPostulacionOFallar(idPostulacion);

        validarEstadoPostulacion(postulacion, ESTADO_PENDIENTE,
                "Solo se pueden pre-aprobar postulaciones en estado 'Pendiente'.");

        postulacion.setEstado(buscarEstadoPorNombre(ESTADO_PRE_APROBADA));
        postulacionRepository.save(postulacion);

        log.info("Postulación {} pre-aprobada. Esperando confirmación del estudiante.", idPostulacion);
    }

    /**
     * Rechazo directo del líder sobre una postulación que ni siquiera llegó a pre-aprobarse.
     */
    public void rechazarPostulacion(Integer idPostulacion) {
        log.info("Rechazando postulación (decisión del líder): {}", idPostulacion);

        Postulacion postulacion = obtenerPostulacionOFallar(idPostulacion);

        validarEstadoPostulacion(postulacion, ESTADO_PENDIENTE,
                "Solo se pueden rechazar postulaciones en estado 'Pendiente'.");

        postulacion.setEstado(buscarEstadoPorNombre(ESTADO_RECHAZADA));
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());
        postulacionRepository.save(postulacion);

        log.info("Postulación {} rechazada por el líder", idPostulacion);
    }

    /**
     * PASO 2 DEL FLUJO DOBLE VÍA, camino negativo (acción del estudiante): rechaza
     * una oferta que el líder ya había pre-aprobado. No toca cupos, porque nunca se
     * llegaron a confirmar/reservar.
     */
    public void rechazarOfertaEstudiante(Integer idPostulacion) {
        log.info("Estudiante rechazando oferta de postulación: {}", idPostulacion);

        Postulacion postulacion = obtenerPostulacionOFallar(idPostulacion);

        validarEstadoPostulacion(postulacion, ESTADO_PRE_APROBADA,
                "Solo se puede rechazar una oferta en estado 'Pre-aprobada'.");

        postulacion.setEstado(buscarEstadoPorNombre(ESTADO_RECHAZADA_ESTUDIANTE));
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());
        postulacionRepository.save(postulacion);

        log.info("Oferta de postulación {} rechazada por el estudiante", idPostulacion);
    }

    /**
     * PASO 2 DEL FLUJO DOBLE VÍA, camino positivo (acción del estudiante): confirma
     * la aceptación de una oferta ya pre-aprobada por el líder. Aquí, y solo aquí,
     * se descuenta el cupo y se dispara la cascada de rechazos sobre las demás
     * postulaciones activas del estudiante.
     */
    public void confirmarAceptacion(Integer idPostulacion) {
        log.info("=== INICIANDO CONFIRMACIÓN DE ACEPTACIÓN: {} ===", idPostulacion);

        Postulacion postulacion = obtenerPostulacionOFallar(idPostulacion);

        validarEstadoPostulacion(postulacion, ESTADO_PRE_APROBADA,
                "Solo se puede confirmar la aceptación de una oferta en estado 'Pre-aprobada'.");

        Convocatoria convocatoria = postulacion.getConvocatoria();

        if (convocatoria.getCuposDisponibles() <= 0) {
            throw new ErrorLogicaNegocioException(
                    "No hay cupos disponibles en la convocatoria: " + convocatoria.getTitulo()
            );
        }

        postulacion.setEstado(buscarEstadoPorNombre(ESTADO_ACEPTADA));
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());

        convocatoria.setCuposDisponibles(convocatoria.getCuposDisponibles() - 1);
        log.info("Cupos decrementados de {} a {}",
                convocatoria.getCuposDisponibles() + 1,
                convocatoria.getCuposDisponibles());

        if (convocatoria.getCuposDisponibles() == 0) {
            convocatoria.setEstado(buscarEstadoPorNombre(ESTADO_CERRADA));
            log.info("Convocatoria marcada como CERRADA (sin cupos)");
        }

        postulacionRepository.save(postulacion);
        convocatoriaRepository.save(convocatoria);

        ejecutarCascadaRechazo(postulacion.getEstudiante().getId(), idPostulacion);

        log.info("=== CONFIRMACIÓN DE ACEPTACIÓN COMPLETADA ===");
    }

    /**
     * LÓGICA DE CASCADA: al confirmar una aceptación, libera todas las demás
     * postulaciones activas del estudiante (tanto las que aún esperaban revisión
     * del líder como las que ya tenía pre-aprobadas), marcándolas como
     * "Rechazada por Estudiante".
     */
    private void ejecutarCascadaRechazo(Integer idEstudiante, Integer idPostulacionAceptada) {
        log.info("Iniciando cascada de rechazos para estudiante {}", idEstudiante);

        Estado estadoRechazadaEstudiante = buscarEstadoPorNombre(ESTADO_RECHAZADA_ESTUDIANTE);

        List<Postulacion> postulacionesActivas = postulacionRepository
                .findPostulacionesActivasByEstudiante(idEstudiante, ESTADO_PENDIENTE, ESTADO_PRE_APROBADA);

        int contadorRechazadas = 0;
        for (Postulacion p : postulacionesActivas) {
            if (!p.getId().equals(idPostulacionAceptada)) {
                log.info("Rechazando (cascada) postulación {} para convocatoria '{}'",
                        p.getId(), p.getConvocatoria().getTitulo());

                p.setEstado(estadoRechazadaEstudiante);
                p.setFechaDecisionEstudiante(ZonedDateTime.now());
                postulacionRepository.save(p);
                contadorRechazadas++;
            }
        }

        log.info("Cascada completada: {} postulaciones rechazadas", contadorRechazadas);
    }

    /**
     * LÓGICA 6: Proceso en segundo plano (cron diario) que cierra convocatorias vencidas
     * y expira todo lo que quedó sin resolver (postulaciones en Pendiente o Pre-aprobada).
     *
     * La expresión cron es configurable vía la propiedad
     * app.scheduler.convocatorias-expiradas.cron (por defecto, 1:00 AM todos los días).
     */
    @Scheduled(cron = "${app.scheduler.convocatorias-expiradas.cron:0 0 1 * * *}")
    public void procesarConvocatoriasExpiradas() {
        log.info("=== INICIANDO PROCESAMIENTO DE CONVOCATORIAS EXPIRADAS ===");

        List<Convocatoria> convocatoriasExpiradas = convocatoriaRepository.findConvocatoriasExpiradas();

        Estado estadoCerrada = buscarEstadoPorNombre(ESTADO_CERRADA);
        Estado estadoExpirada = buscarEstadoPorNombre(ESTADO_EXPIRADA);

        int convocatoriasActualizadas = 0;
        for (Convocatoria conv : convocatoriasExpiradas) {
            log.info("Cerrando convocatoria expirada: {} (fecha_cierre: {})",
                    conv.getTitulo(), conv.getFechaCierre());

            conv.setEstado(estadoCerrada);
            convocatoriaRepository.save(conv);
            convocatoriasActualizadas++;

            // Postulaciones que quedaron sin resolver a tiempo: en revisión del líder
            // (Pendiente) o esperando confirmación del estudiante (Pre-aprobada)
            List<Postulacion> postulacionesSinResolver = postulacionRepository
                    .findPostulacionesByConvocatoriaAndEstado(conv.getId(), ESTADO_PENDIENTE, ESTADO_PRE_APROBADA);

            for (Postulacion post : postulacionesSinResolver) {
                log.info("Marcando postulación {} como expirada", post.getId());
                post.setEstado(estadoExpirada);
                post.setFechaDecisionEstudiante(ZonedDateTime.now());
                postulacionRepository.save(post);
            }
        }

        log.info("=== PROCESAMIENTO COMPLETADO: {} convocatorias cerradas ===", convocatoriasActualizadas);
    }

    /**
     * Obtiene el estado de cupos de una convocatoria
     */
    public ConvocatoriaEstadoCuposDTO obtenerEstadoCupos(Integer idConvocatoria) {
        Convocatoria convocatoria = convocatoriaRepository.findById(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));

        int cuposUtilizados = convocatoria.getCuposTotales() - convocatoria.getCuposDisponibles();

        return ConvocatoriaEstadoCuposDTO.builder()
                .idConvocatoria(idConvocatoria)
                .nombreConvocatoria(convocatoria.getTitulo())
                .cuposOriginales(convocatoria.getCuposTotales())
                .cuposDisponibles(convocatoria.getCuposDisponibles())
                .cuposUtilizados(cuposUtilizados)
                .porcentajeOcupacion((cuposUtilizados * 100.0) / convocatoria.getCuposTotales())
                .build();
    }

    private Postulacion obtenerPostulacionOFallar(Integer idPostulacion) {
        return postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion));
    }

    private void validarEstadoPostulacion(Postulacion postulacion, String estadoEsperado, String mensaje) {
        if (!postulacion.getEstado().getNombre().equalsIgnoreCase(estadoEsperado)) {
            throw new ErrorLogicaNegocioException(
                    mensaje + " Estado actual: " + postulacion.getEstado().getNombre()
            );
        }
    }

    private Estado buscarEstadoPorNombre(String nombre) {
        return estadoRepository.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", nombre));
    }

    /**
     * DTO para respuesta de estado de cupos
     */
    @lombok.Data
    @lombok.Builder
    public static class ConvocatoriaEstadoCuposDTO {
        private Integer idConvocatoria;
        private String nombreConvocatoria;
        private Integer cuposOriginales;
        private Integer cuposDisponibles;
        private Integer cuposUtilizados;
        private Double porcentajeOcupacion;
    }
}
