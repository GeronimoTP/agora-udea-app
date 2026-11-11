package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosSemillero;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.evento.SemillerosModificadosEvent;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.mapper.SemilleroMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Aprobación de semilleros por el administrador (decisión de la reunión con el profesor).
 *
 * <pre>
 *   registro --> Pendiente de aprobación --(aprueba)--> Activo
 *                      ^         |
 *          (reenvía)   |         +--(rechaza con motivo)--> Rechazado
 *                      +-----------------------------------------+
 * </pre>
 *
 * Mientras no está aprobado, el semillero solo lo ven sus líderes y el administrador, no abre
 * convocatorias ni registra integrantes, proyectos o reconocimientos. Sus líderes pueden
 * corregir los datos, reenviarlo si fue rechazado o eliminarlo.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AprobacionSemillerosService {

    private final SemilleroRepository semilleroRepository;
    private final BuscadorEstados buscadorEstados;
    private final SemilleroMapper semilleroMapper;
    private final ApplicationEventPublisher eventos;

    /** Solicitudes que esperan revisión, las más antiguas primero. */
    @Transactional(readOnly = true)
    public List<SemilleroResponseDTO> pendientes() {
        return semilleroMapper.aResponses(semilleroRepository.findByNombreEstado(EstadosSemillero.PENDIENTE_APROBACION));
    }

    public SemilleroResponseDTO aprobar(Integer idSemillero, Usuario administrador) {
        Semillero semillero = pendiente(idSemillero);
        semillero.setEstado(buscadorEstados.deSemillero(EstadosSemillero.ACTIVO));
        registrarRevision(semillero, administrador, null);
        // Un semillero activo entra al árbol KD de semilleros similares
        eventos.publishEvent(new SemillerosModificadosEvent());
        log.info("Semillero {} aprobado por el usuario {}", idSemillero, administrador.getId());
        return semilleroMapper.aResponse(semilleroRepository.save(semillero));
    }

    public SemilleroResponseDTO rechazar(Integer idSemillero, String motivo, Usuario administrador) {
        Semillero semillero = pendiente(idSemillero);
        semillero.setEstado(buscadorEstados.deSemillero(EstadosSemillero.RECHAZADO));
        registrarRevision(semillero, administrador, motivo.trim());
        log.info("Semillero {} rechazado por el usuario {}", idSemillero, administrador.getId());
        return semilleroMapper.aResponse(semilleroRepository.save(semillero));
    }

    /** Un líder reenvía un semillero rechazado, normalmente después de corregirlo. */
    public SemilleroResponseDTO reenviar(Integer idSemillero) {
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        if (!semillero.fueRechazado()) {
            throw new ErrorLogicaNegocioException("Solo se puede reenviar un semillero rechazado.");
        }
        semillero.setEstado(buscadorEstados.deSemillero(EstadosSemillero.PENDIENTE_APROBACION));
        // El motivo del rechazo anterior se conserva hasta la nueva revisión, como referencia
        semillero.setFechaSolicitud(ZonedDateTime.now());
        return semilleroMapper.aResponse(semilleroRepository.save(semillero));
    }

    private Semillero pendiente(Integer idSemillero) {
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        if (!semillero.estaPendienteDeAprobacion()) {
            throw new ErrorLogicaNegocioException(
                    "El semillero no está pendiente de aprobación. Estado actual: " + semillero.getEstado().getNombre());
        }
        return semillero;
    }

    private static void registrarRevision(Semillero semillero, Usuario administrador, String motivoRechazo) {
        semillero.setFechaRevision(ZonedDateTime.now());
        semillero.setUsuarioRevisor(administrador);
        semillero.setMotivoRechazo(motivoRechazo);
    }
}
