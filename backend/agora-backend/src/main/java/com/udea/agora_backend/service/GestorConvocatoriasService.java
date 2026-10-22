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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

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
 *   Al aceptar: las demás postulaciones activas del estudiante pasan a Cancelada (cascada) y
 *   el estudiante queda vinculado al semillero. Lo que queda sin resolver al cerrarse la
 *   convocatoria (sin cupos o por fecha) pasa a Expirada.
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
    private static final String ESTADO_CANCELADA = "Cancelada en cascada";
    private static final String ESTADO_ABIERTA = "Abierta";
    private static final String ESTADO_CONVOCATORIA_CANCELADA = "Cancelada";
    private static final String ESTADO_VINCULACION_ACTIVA = "Activo";

    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EstadoRepository estadoRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;

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
        validarConvocatoriaVigente(postulacion.getConvocatoria());

        postulacion.setEstado(buscarEstado(CategoriasEstado.POSTULACION, ESTADO_PRE_APROBADA));
        postulacion.setFechaPreAprobacion(ZonedDateTime.now());
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

        // Decisión del líder: no es una respuesta del estudiante, no se registra fechaDecisionEstudiante
        postulacion.setEstado(buscarEstado(CategoriasEstado.POSTULACION, ESTADO_RECHAZADA));
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

        postulacion.setEstado(buscarEstado(CategoriasEstado.POSTULACION, ESTADO_RECHAZADA_ESTUDIANTE));
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

        // Bloquea la fila de la convocatoria hasta el commit: dos aceptaciones simultáneas
        // no pueden leer el mismo cupo disponible
        Convocatoria convocatoria = convocatoriaRepository
                .findByIdParaActualizarCupos(postulacion.getConvocatoria().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", postulacion.getConvocatoria().getId()));
        validarConvocatoriaVigente(convocatoria);

        if (convocatoria.getCuposDisponibles() <= 0) {
            throw new ErrorLogicaNegocioException(
                    "No hay cupos disponibles en la convocatoria: " + convocatoria.getTitulo()
            );
        }

        postulacion.setEstado(buscarEstado(CategoriasEstado.POSTULACION, ESTADO_ACEPTADA));
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());

        convocatoria.setCuposDisponibles(convocatoria.getCuposDisponibles() - 1);
        log.info("Cupos decrementados de {} a {}",
                convocatoria.getCuposDisponibles() + 1,
                convocatoria.getCuposDisponibles());

        postulacionRepository.save(postulacion);

        vincularEstudianteAlSemillero(postulacion.getEstudiante(), convocatoria.getSemillero());

        ejecutarCascadaRechazo(postulacion.getEstudiante().getId(), idPostulacion);

        if (convocatoria.getCuposDisponibles() == 0) {
            convocatoria.setEstado(buscarEstado(CategoriasEstado.CONVOCATORIA, ESTADO_CERRADA));
            log.info("Convocatoria marcada como CERRADA (sin cupos)");
            expirarPostulacionesSinResolver(convocatoria.getId());
        }
        convocatoriaRepository.save(convocatoria);

        log.info("=== CONFIRMACIÓN DE ACEPTACIÓN COMPLETADA ===");
    }

    /**
     * LÓGICA DE CASCADA: al confirmar una aceptación, libera todas las demás
     * postulaciones activas del estudiante (tanto las que aún esperaban revisión
     * del líder como las que ya tenía pre-aprobadas), marcándolas como "Cancelada en cascada".
     * Se distingue de "Rechazada por Estudiante", que es una decisión explícita.
     */
    private void ejecutarCascadaRechazo(Integer idEstudiante, Integer idPostulacionAceptada) {
        log.info("Iniciando cascada de rechazos para estudiante {}", idEstudiante);

        Estado estadoCancelada = buscarEstado(CategoriasEstado.POSTULACION, ESTADO_CANCELADA);

        List<Postulacion> postulacionesActivas = postulacionRepository
                .findPostulacionesActivasByEstudiante(idEstudiante, ESTADO_PENDIENTE, ESTADO_PRE_APROBADA);

        int contadorRechazadas = 0;
        for (Postulacion p : postulacionesActivas) {
            if (!p.getId().equals(idPostulacionAceptada)) {
                log.info("Rechazando (cascada) postulación {} para convocatoria '{}'",
                        p.getId(), p.getConvocatoria().getTitulo());

                p.setEstado(estadoCancelada);
                postulacionRepository.save(p);
                contadorRechazadas++;
            }
        }

        log.info("Cascada completada: {} postulaciones canceladas", contadorRechazadas);
    }

    /**
     * Cierre anticipado por el líder: la convocatoria deja de recibir postulaciones y lo que
     * quedaba en revisión o con oferta pendiente expira. Los cupos ya asignados se conservan.
     */
    public void cerrarConvocatoria(Integer idConvocatoria) {
        finalizarConvocatoria(idConvocatoria, ESTADO_CERRADA);
    }

    /**
     * Cancelación por el líder: la convocatoria no se llevará a cabo. Igual que el cierre,
     * expira lo que quedaba sin resolver; las aceptaciones ya confirmadas se mantienen.
     */
    public void cancelarConvocatoria(Integer idConvocatoria) {
        finalizarConvocatoria(idConvocatoria, ESTADO_CONVOCATORIA_CANCELADA);
    }

    private void finalizarConvocatoria(Integer idConvocatoria, String estadoFinal) {
        Convocatoria convocatoria = convocatoriaRepository.findByIdParaActualizarCupos(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));
        if (!ESTADO_ABIERTA.equalsIgnoreCase(convocatoria.getEstado().getNombre())) {
            throw new ErrorLogicaNegocioException(
                    "Solo se puede cerrar o cancelar una convocatoria abierta. Estado actual: "
                            + convocatoria.getEstado().getNombre());
        }
        convocatoria.setEstado(buscarEstado(CategoriasEstado.CONVOCATORIA, estadoFinal));
        convocatoriaRepository.save(convocatoria);
        int expiradas = expirarPostulacionesSinResolver(idConvocatoria);
        log.info("Convocatoria {} pasó a '{}' por decisión del líder; {} postulaciones expiradas",
                idConvocatoria, estadoFinal, expiradas);
    }

    /**
     * Un estudiante que acepta una oferta pasa a ser miembro del semillero.
     * Si había sido retirado antes, su vinculación se reactiva.
     */
    private void vincularEstudianteAlSemillero(Estudiante estudiante, Semillero semillero) {
        Optional<EstudianteSemillero> existente = estudianteSemilleroRepository
                .findBySemilleroIdAndEstudianteId(semillero.getId(), estudiante.getId());
        if (existente.isPresent()) {
            EstudianteSemillero vinculacion = existente.get();
            if (vinculacion.getFechaSalida() != null) {
                vinculacion.setFechaSalida(null);
                vinculacion.setFechaIngreso(LocalDate.now());
                vinculacion.setEstado(buscarEstado(CategoriasEstado.VINCULACION, ESTADO_VINCULACION_ACTIVA));
                estudianteSemilleroRepository.save(vinculacion);
                log.info("Estudiante {} reincorporado al semillero {}", estudiante.getId(), semillero.getId());
            }
            return;
        }
        estudianteSemilleroRepository.save(EstudianteSemillero.builder()
                .semillero(semillero)
                .estudiante(estudiante)
                .fechaIngreso(LocalDate.now())
                .estado(buscarEstado(CategoriasEstado.VINCULACION, ESTADO_VINCULACION_ACTIVA))
                .build());
        log.info("Estudiante {} vinculado al semillero {}", estudiante.getId(), semillero.getId());
    }

    /**
     * Expira lo que quedó en revisión o con oferta pendiente en una convocatoria cerrada.
     */
    private int expirarPostulacionesSinResolver(Integer idConvocatoria) {
        Estado estadoExpirada = buscarEstado(CategoriasEstado.POSTULACION, ESTADO_EXPIRADA);
        List<Postulacion> sinResolver = postulacionRepository
                .findPostulacionesByConvocatoriaAndEstado(idConvocatoria, ESTADO_PENDIENTE, ESTADO_PRE_APROBADA);
        for (Postulacion post : sinResolver) {
            log.info("Marcando postulación {} como expirada", post.getId());
            post.setEstado(estadoExpirada);
            postulacionRepository.save(post);
        }
        return sinResolver.size();
    }

    private void validarConvocatoriaVigente(Convocatoria convocatoria) {
        if (!ESTADO_ABIERTA.equalsIgnoreCase(convocatoria.getEstado().getNombre())
                || convocatoria.getFechaCierre().isBefore(LocalDate.now())) {
            throw new ErrorLogicaNegocioException(
                    "La convocatoria '" + convocatoria.getTitulo() + "' ya no está abierta.");
        }
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

        Estado estadoCerrada = buscarEstado(CategoriasEstado.CONVOCATORIA, ESTADO_CERRADA);

        int convocatoriasActualizadas = 0;
        for (Convocatoria conv : convocatoriasExpiradas) {
            log.info("Cerrando convocatoria expirada: {} (fecha_cierre: {})",
                    conv.getTitulo(), conv.getFechaCierre());

            conv.setEstado(estadoCerrada);
            convocatoriaRepository.save(conv);
            convocatoriasActualizadas++;

            expirarPostulacionesSinResolver(conv.getId());
        }

        // Convocatorias que ya estaban cerradas (sin cupos o cierre manual) pero conservan
        // postulaciones sin resolver: también expiran
        Estado estadoExpirada = buscarEstado(CategoriasEstado.POSTULACION, ESTADO_EXPIRADA);
        for (Postulacion post : postulacionRepository.findPostulacionesEnConvocatoriasConEstado(
                ESTADO_CERRADA, ESTADO_PENDIENTE, ESTADO_PRE_APROBADA)) {
            post.setEstado(estadoExpirada);
            postulacionRepository.save(post);
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

    private Estado buscarEstado(String categoria, String nombre) {
        return estadoRepository.findByCategoriaEstadoNombreIgnoreCaseAndNombreIgnoreCase(categoria, nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", categoria + " / " + nombre));
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
