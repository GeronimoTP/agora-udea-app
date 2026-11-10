package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosConvocatoria;
import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.dominio.EstadosVinculacion;
import com.udea.agora_backend.dto.response.ConvocatoriaEstadoCuposDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
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
 * 7. Límite de ofertas: las ofertas vigentes de una convocatoria nunca superan sus cupos disponibles,
 *    así toda oferta que el estudiante acepte a tiempo tiene cupo garantizado
 * 8. Plazo de respuesta: una oferta vence a los N días de emitida (PlazoOfertasService); un cron
 *    la marca como Expirada y libera el espacio para otro aspirante
 *
 * La priorización de candidatos (PriorizacionCandidatosService) completa este servicio.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GestorConvocatoriasService {

    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final BuscadorEstados buscadorEstados;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final PlazoOfertasService plazoOfertasService;

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
        Convocatoria convocatoria = convocatoriaRepository
                .findByIdParaActualizarCupos(postulacion.getConvocatoria().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", postulacion.getConvocatoria().getId()));
        validarConvocatoriaVigente(convocatoria);

        ZonedDateTime ahora = ZonedDateTime.now();
        // Las ofertas vencidas que el cron aún no procesó no deben ocupar espacio
        expirarOfertasVencidas(postulacionRepository.findOfertasVencidasDeConvocatoria(
                convocatoria.getId(), EstadosPostulacion.PRE_APROBADA, plazoOfertasService.inicioOfertasVigentes(ahora)));
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
        Convocatoria convocatoria = convocatoriaRepository
                .findByIdParaActualizarCupos(postulacion.getConvocatoria().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", postulacion.getConvocatoria().getId()));
        validarConvocatoriaVigente(convocatoria);

        if (convocatoria.getCuposDisponibles() <= 0) {
            throw new ErrorLogicaNegocioException(
                    "No hay cupos disponibles en la convocatoria: " + convocatoria.getTitulo()
            );
        }

        postulacion.setEstado(buscadorEstados.dePostulacion(EstadosPostulacion.ACEPTADA));
        postulacion.setFechaDecisionEstudiante(ZonedDateTime.now());

        convocatoria.setCuposDisponibles(convocatoria.getCuposDisponibles() - 1);
        log.info("Cupos decrementados de {} a {}",
                convocatoria.getCuposDisponibles() + 1,
                convocatoria.getCuposDisponibles());

        postulacionRepository.save(postulacion);

        vincularEstudianteAlSemillero(postulacion.getEstudiante(), convocatoria.getSemillero());

        ejecutarCascadaRechazo(postulacion.getEstudiante().getId(), idPostulacion);

        if (convocatoria.getCuposDisponibles() == 0) {
            convocatoria.setEstado(buscadorEstados.deConvocatoria(EstadosConvocatoria.CERRADA));
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

        Estado estadoCancelada = buscadorEstados.dePostulacion(EstadosPostulacion.CANCELADA_EN_CASCADA);

        List<Postulacion> postulacionesActivas = postulacionRepository
                .findPostulacionesActivasByEstudiante(idEstudiante, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA);

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
            if (!vinculacion.estaActiva()) {
                vinculacion.setFechaSalida(null);
                vinculacion.setFechaIngreso(LocalDate.now());
                vinculacion.setEstado(buscadorEstados.deVinculacion(EstadosVinculacion.ACTIVA));
                estudianteSemilleroRepository.save(vinculacion);
                log.info("Estudiante {} reincorporado al semillero {}", estudiante.getId(), semillero.getId());
            }
            return;
        }
        estudianteSemilleroRepository.save(EstudianteSemillero.builder()
                .semillero(semillero)
                .estudiante(estudiante)
                .fechaIngreso(LocalDate.now())
                .estado(buscadorEstados.deVinculacion(EstadosVinculacion.ACTIVA))
                .build());
        log.info("Estudiante {} vinculado al semillero {}", estudiante.getId(), semillero.getId());
    }

    /**
     * Expira lo que quedó en revisión o con oferta pendiente en una convocatoria cerrada.
     */
    private int expirarPostulacionesSinResolver(Integer idConvocatoria) {
        Estado estadoExpirada = buscadorEstados.dePostulacion(EstadosPostulacion.EXPIRADA);
        List<Postulacion> sinResolver = postulacionRepository
                .findPostulacionesByConvocatoriaAndEstado(idConvocatoria, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA);
        for (Postulacion post : sinResolver) {
            log.info("Marcando postulación {} como expirada", post.getId());
            post.setEstado(estadoExpirada);
            postulacionRepository.save(post);
        }
        return sinResolver.size();
    }

    // Una oferta vencida no se puede aceptar ni declinar; el cron la marcará como Expirada
    private void validarOfertaEnPlazo(Postulacion postulacion) {
        if (plazoOfertasService.estaVencida(postulacion, ZonedDateTime.now())) {
            throw new ErrorLogicaNegocioException(
                    "El plazo de " + plazoOfertasService.getDiasRespuesta()
                            + " día(s) para responder esta oferta ya venció.");
        }
    }

    private int expirarOfertasVencidas(List<Postulacion> vencidas) {
        if (vencidas.isEmpty()) {
            return 0;
        }
        Estado estadoExpirada = buscadorEstados.dePostulacion(EstadosPostulacion.EXPIRADA);
        for (Postulacion oferta : vencidas) {
            log.info("Oferta {} expirada: el estudiante no respondió en el plazo", oferta.getId());
            oferta.setEstado(estadoExpirada);
            postulacionRepository.save(oferta);
        }
        return vencidas.size();
    }

    /**
     * Cron (por defecto cada hora, app.scheduler.ofertas-vencidas.cron) que expira las ofertas
     * cuyo plazo de respuesta venció. Conserva fechaPreAprobacion: cuenta como oferta expirada
     * en la analítica de retención.
     */
    @Scheduled(cron = "${app.scheduler.ofertas-vencidas.cron:0 0 * * * *}")
    public void procesarOfertasVencidas() {
        int expiradas = expirarOfertasVencidas(postulacionRepository.findOfertasVencidas(
                EstadosPostulacion.PRE_APROBADA, plazoOfertasService.inicioOfertasVigentes(ZonedDateTime.now())));
        if (expiradas > 0) {
            log.info("{} ofertas expiradas por vencimiento del plazo de respuesta", expiradas);
        }
    }

    private void validarConvocatoriaVigente(Convocatoria convocatoria) {
        if (!convocatoria.estaVigente(LocalDate.now())) {
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
        procesarOfertasVencidas();

        List<Convocatoria> convocatoriasExpiradas = convocatoriaRepository.findConvocatoriasExpiradas();

        Estado estadoCerrada = buscadorEstados.deConvocatoria(EstadosConvocatoria.CERRADA);

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
        Estado estadoExpirada = buscadorEstados.dePostulacion(EstadosPostulacion.EXPIRADA);
        for (Postulacion post : postulacionRepository.findPostulacionesEnConvocatoriasConEstado(
                EstadosConvocatoria.CERRADA, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA)) {
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
        if (!postulacion.tieneEstado(estadoEsperado)) {
            throw new ErrorLogicaNegocioException(
                    mensaje + " Estado actual: " + postulacion.getEstado().getNombre()
            );
        }
    }

}
