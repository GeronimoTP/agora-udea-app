package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosConvocatoria;
import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * SERVICIO CRÍTICO: Gestor de Convocatorias. Implementa el Flujo de Selección de Doble Vía:
 *
 * <pre>
 *   Pendiente --(líder pre-aprueba)--> Pre-aprobada --(estudiante confirma)--> Aceptada
 *      |                                    |
 *      +--(líder rechaza)--> Rechazada      +--(estudiante rechaza oferta)--> Rechazada por Estudiante
 * </pre>
 *
 * <ol>
 *   <li>Postulación concurrente: un estudiante puede tener varias postulaciones activas.</li>
 *   <li>Doble vía: el líder pre-aprueba (filtra candidatos) y el estudiante decide.</li>
 *   <li>Varios semilleros: un estudiante puede integrar varios semilleros, así que aceptar una
 *       oferta NO cancela sus postulaciones a otros semilleros. Solo pasan a "Cancelada en
 *       cascada" sus otras postulaciones en proceso al MISMO semillero, que ya integra.</li>
 *   <li>Cupos dinámicos: se descuentan solo con la confirmación del estudiante; con 0 cupos
 *       la convocatoria se cierra.</li>
 *   <li>Límite de ofertas: las ofertas vigentes nunca superan los cupos disponibles, así toda
 *       oferta aceptada a tiempo tiene cupo garantizado.</li>
 *   <li>Plazo de respuesta: una oferta vence a los N días de emitida ({@link PlazoOfertasService}).</li>
 * </ol>
 *
 * <p>Lo que queda sin resolver al cerrarse una convocatoria pasa a Expirada
 * ({@link ExpiracionPostulacionesService}); los cierres por fecha y las ofertas vencidas los
 * procesan las tareas programadas de {@code ExpiracionConvocatoriasJob}. La priorización de
 * candidatos ({@link PriorizacionCandidatosService}) completa este servicio.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GestorConvocatoriasService {

    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final BuscadorEstados buscadorEstados;
    private final PlazoOfertasService plazoOfertasService;
    private final ExpiracionPostulacionesService expiracionPostulaciones;
    private final MiembrosSemilleroService miembrosSemilleroService;

    /**
     * PASO 1 DEL FLUJO DOBLE VÍA (acción del líder): pre-aprueba una postulación.
     * No mueve cupos ni dispara cascada; solo indica que el líder filtró y quiere
     * ofrecerle el cupo al estudiante. La decisión final queda en manos del estudiante.
     */
    public void preAprobarPostulacion(Integer idPostulacion) {
        log.info("Pre-aprobando postulación: {}", idPostulacion);

        Postulacion postulacion = obtenerPostulacionOFallar(idPostulacion);
        validarEstadoPostulacion(postulacion, EstadosPostulacion.PENDIENTE,
                "Solo se pueden pre-aprobar postulaciones en estado 'Pendiente'.");

        // Bloquea la convocatoria: dos pre-aprobaciones simultáneas no pueden exceder los cupos
        Convocatoria convocatoria = bloquearConvocatoriaVigente(postulacion);

        ZonedDateTime ahora = ZonedDateTime.now();
        expiracionPostulaciones.expirarOfertasVencidasDeConvocatoria(convocatoria.getId());
        long ofertasVigentes = postulacionRepository.contarOfertasVigentes(
                convocatoria.getId(), EstadosPostulacion.PRE_APROBADA, plazoOfertasService.inicioOfertasVigentes(ahora));
        if (ofertasVigentes >= convocatoria.getCuposDisponibles()) {
            throw new ErrorLogicaNegocioException(
                    "Ya hay " + ofertasVigentes + " oferta(s) vigente(s) para " + convocatoria.getCuposDisponibles()
                            + " cupo(s) disponible(s). Espera a que los estudiantes respondan o a que venza su plazo.");
        }

        postulacion.setEstado(buscadorEstados.dePostulacion(EstadosPostulacion.PRE_APROBADA));
        postulacion.setFechaPreAprobacion(ahora);
        postulacionRepository.save(postulacion);

        log.info("Postulación {} pre-aprobada. Esperando confirmación del estudiante.", idPostulacion);
    }

    /**
     * Rechazo directo del líder sobre una postulación que ni siquiera llegó a pre-aprobarse.
     */
    public void rechazarPostulacion(Integer idPostulacion) {
        log.info("Rechazando postulación (decisión del líder): {}", idPostulacion);

        Postulacion postulacion = obtenerPostulacionOFallar(idPostulacion);
        validarEstadoPostulacion(postulacion, EstadosPostulacion.PENDIENTE,
                "Solo se pueden rechazar postulaciones en estado 'Pendiente'.");

        // Decisión del líder: no es una respuesta del estudiante, no se registra fechaDecisionEstudiante
        postulacion.setEstado(buscadorEstados.dePostulacion(EstadosPostulacion.RECHAZADA));
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
        validarEstadoPostulacion(postulacion, EstadosPostulacion.PRE_APROBADA,
                "Solo se puede rechazar una oferta en estado 'Pre-aprobada'.");
        validarOfertaEnPlazo(postulacion);

        postulacion.setEstado(buscadorEstados.dePostulacion(EstadosPostulacion.RECHAZADA_POR_ESTUDIANTE));
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
        validarEstadoPostulacion(postulacion, EstadosPostulacion.PRE_APROBADA,
                "Solo se puede confirmar la aceptación de una oferta en estado 'Pre-aprobada'.");
        validarOfertaEnPlazo(postulacion);

        // Bloquea la fila de la convocatoria hasta el commit: dos aceptaciones simultáneas
        // no pueden leer el mismo cupo disponible
        Convocatoria convocatoria = bloquearConvocatoriaVigente(postulacion);
        if (!convocatoria.tieneCuposDisponibles()) {
            throw new ErrorLogicaNegocioException(
                    "No hay cupos disponibles en la convocatoria: " + convocatoria.getTitulo());
        }

        postulacion.setEstado(buscadorEstados.dePostulacion(EstadosPostulacion.ACEPTADA));
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());
        postulacionRepository.save(postulacion);

        convocatoria.setCuposDisponibles(convocatoria.getCuposDisponibles() - 1);
        log.info("Cupos decrementados de {} a {}",
                convocatoria.getCuposDisponibles() + 1, convocatoria.getCuposDisponibles());

        miembrosSemilleroService.incorporarAdmitido(postulacion.getEstudiante(), convocatoria.getSemillero());
        cancelarOtrasAlMismoSemillero(postulacion.getEstudiante().getId(), idPostulacion,
                convocatoria.getSemillero().getId());

        if (!convocatoria.tieneCuposDisponibles()) {
            convocatoria.setEstado(buscadorEstados.deConvocatoria(EstadosConvocatoria.CERRADA));
            log.info("Convocatoria marcada como CERRADA (sin cupos)");
            expiracionPostulaciones.expirarSinResolver(convocatoria.getId());
        }
        convocatoriaRepository.save(convocatoria);

        log.info("=== CONFIRMACIÓN DE ACEPTACIÓN COMPLETADA ===");
    }

    /**
     * Cierre anticipado por el líder: la convocatoria deja de recibir postulaciones y lo que
     * quedaba en revisión o con oferta pendiente expira. Los cupos ya asignados se conservan.
     */
    public void cerrarConvocatoria(Integer idConvocatoria) {
        finalizarConvocatoria(idConvocatoria, EstadosConvocatoria.CERRADA);
    }

    /**
     * Cancelación por el líder: la convocatoria no se llevará a cabo. Igual que el cierre,
     * expira lo que quedaba sin resolver; las aceptaciones ya confirmadas se mantienen.
     */
    public void cancelarConvocatoria(Integer idConvocatoria) {
        finalizarConvocatoria(idConvocatoria, EstadosConvocatoria.CANCELADA);
    }

    private void finalizarConvocatoria(Integer idConvocatoria, String estadoFinal) {
        Convocatoria convocatoria = convocatoriaRepository.findByIdParaActualizarCupos(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));
        if (!convocatoria.estaAbierta()) {
            throw new ErrorLogicaNegocioException(
                    "Solo se puede cerrar o cancelar una convocatoria abierta. Estado actual: "
                            + convocatoria.getEstado().getNombre());
        }
        convocatoria.setEstado(buscadorEstados.deConvocatoria(estadoFinal));
        convocatoriaRepository.save(convocatoria);
        int expiradas = expiracionPostulaciones.expirarSinResolver(idConvocatoria);
        log.info("Convocatoria {} pasó a '{}' por decisión del líder; {} postulaciones expiradas",
                idConvocatoria, estadoFinal, expiradas);
    }

    /**
     * Al confirmar una aceptación, las demás postulaciones en proceso del estudiante (en
     * revisión o con oferta) a otras convocatorias del MISMO semillero pasan a "Cancelada en
     * cascada": ya es integrante y no puede volver a postularse a él. Sus postulaciones a otros
     * semilleros siguen su curso, porque puede integrar varios a la vez. Se distingue de
     * "Rechazada por Estudiante", que es una decisión explícita.
     */
    private void cancelarOtrasAlMismoSemillero(Integer idEstudiante, Integer idPostulacionAceptada,
                                               Integer idSemillero) {
        Estado canceladaEnCascada = buscadorEstados.dePostulacion(EstadosPostulacion.CANCELADA_EN_CASCADA);
        List<Postulacion> otrasActivas = postulacionRepository
                .findPostulacionesActivasByEstudiante(idEstudiante, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA)
                .stream()
                .filter(p -> !p.getId().equals(idPostulacionAceptada))
                .filter(p -> p.getConvocatoria().getSemillero().getId().equals(idSemillero))
                .toList();

        for (Postulacion otra : otrasActivas) {
            log.info("Rechazando (cascada) postulación {} para convocatoria '{}'",
                    otra.getId(), otra.getConvocatoria().getTitulo());
            otra.setEstado(canceladaEnCascada);
            postulacionRepository.save(otra);
        }

        log.info("Estudiante {}: {} postulaciones al semillero {} canceladas en cascada",
                idEstudiante, otrasActivas.size(), idSemillero);
    }

    /** Bloquea la convocatoria de la postulación (SELECT ... FOR UPDATE) y exige que siga vigente. */
    private Convocatoria bloquearConvocatoriaVigente(Postulacion postulacion) {
        Integer idConvocatoria = postulacion.getConvocatoria().getId();
        Convocatoria convocatoria = convocatoriaRepository.findByIdParaActualizarCupos(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));
        if (!convocatoria.estaVigente(LocalDate.now())) {
            throw new ErrorLogicaNegocioException(
                    "La convocatoria '" + convocatoria.getTitulo() + "' ya no está abierta.");
        }
        return convocatoria;
    }

    // Una oferta vencida no se puede aceptar ni declinar; el cron la marcará como Expirada
    private void validarOfertaEnPlazo(Postulacion postulacion) {
        if (plazoOfertasService.estaVencida(postulacion, ZonedDateTime.now())) {
            throw new ErrorLogicaNegocioException(
                    "El plazo de " + plazoOfertasService.getDiasRespuesta()
                            + " día(s) para responder esta oferta ya venció.");
        }
    }

    private Postulacion obtenerPostulacionOFallar(Integer idPostulacion) {
        return postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion));
    }

    private void validarEstadoPostulacion(Postulacion postulacion, String estadoEsperado, String mensaje) {
        if (!postulacion.tieneEstado(estadoEsperado)) {
            throw new ErrorLogicaNegocioException(mensaje + " Estado actual: " + postulacion.getEstado().getNombre());
        }
    }
}
