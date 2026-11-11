package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosSolicitud;
import com.udea.agora_backend.dto.response.SolicitudRetiroResponseDTO;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.SolicitudRetiro;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.SolicitudRetiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Retiro voluntario (decisión de la reunión con el profesor). El integrante no sale de
 * inmediato: envía una solicitud con su motivo y un líder del semillero (estudiante líder o
 * tutor) la aprueba o la rechaza, para poder hablar antes con él.
 * <ul>
 *   <li>Aprobada: el integrante queda retirado (fecha de salida hoy, como el retiro por el líder).</li>
 *   <li>Rechazada: con un comentario obligatorio; sigue siendo integrante y puede volver a solicitarlo.</li>
 * </ul>
 * Solo puede haber una solicitud pendiente por integrante y semillero. El estudiante líder no
 * puede solicitar su retiro: primero debe transferir el liderazgo.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SolicitudRetiroService {

    private final SolicitudRetiroRepository solicitudRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final MiembrosSemilleroService miembrosSemilleroService;
    private final BuscadorEstados buscadorEstados;

    public SolicitudRetiroResponseDTO solicitar(Integer idEstudiante, Integer idSemillero, String motivo) {
        EstudianteSemillero vinculacion = estudianteSemilleroRepository
                .findBySemilleroIdAndEstudianteId(idSemillero, idEstudiante)
                .filter(EstudianteSemillero::estaActiva)
                .orElseThrow(() -> new ErrorLogicaNegocioException("No eres integrante activo de este semillero."));
        if (vinculacion.getSemillero().getEstudianteLider().getId().equals(idEstudiante)) {
            throw new ErrorLogicaNegocioException(
                    "Eres el líder del semillero: transfiere el liderazgo antes de solicitar tu retiro.");
        }
        if (!solicitudRepository.findByVinculacionYEstado(vinculacion.getId(), EstadosSolicitud.PENDIENTE).isEmpty()) {
            throw new ErrorLogicaNegocioException("Ya tienes una solicitud de retiro pendiente en este semillero.");
        }
        SolicitudRetiro solicitud = solicitudRepository.save(SolicitudRetiro.builder()
                .vinculacion(vinculacion)
                .motivo(motivo.trim())
                .estado(buscadorEstados.deSolicitud(EstadosSolicitud.PENDIENTE))
                .fechaSolicitud(ZonedDateTime.now())
                .build());
        log.info("Estudiante {} solicitó retirarse del semillero {}", idEstudiante, idSemillero);
        return aResponse(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudRetiroResponseDTO> delEstudiante(Integer idEstudiante) {
        return solicitudRepository.findByEstudiante(idEstudiante).stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudRetiroResponseDTO> delSemillero(Integer idSemillero) {
        return solicitudRepository.findBySemillero(idSemillero).stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public Integer idSemilleroDe(Integer idSolicitud) {
        return buscar(idSolicitud).getVinculacion().getSemillero().getId();
    }

    @Transactional(readOnly = true)
    public long contarPendientes(Collection<Integer> idsSemillero) {
        return idsSemillero.isEmpty() ? 0
                : solicitudRepository.contarPorSemillerosYEstado(idsSemillero, EstadosSolicitud.PENDIENTE);
    }

    /** El líder acepta: el integrante queda retirado del semillero. */
    public SolicitudRetiroResponseDTO aprobar(Integer idSolicitud, Usuario lider, String comentario) {
        SolicitudRetiro solicitud = pendiente(idSolicitud);
        responder(solicitud, EstadosSolicitud.APROBADA, lider, comentario);
        EstudianteSemillero vinculacion = solicitud.getVinculacion();
        miembrosSemilleroService.retirarEstudiante(vinculacion.getSemillero().getId(), vinculacion.getEstudiante().getId());
        return aResponse(solicitud);
    }

    /** El líder no acepta todavía (por ejemplo, para hablar antes): el estudiante sigue como integrante. */
    public SolicitudRetiroResponseDTO rechazar(Integer idSolicitud, Usuario lider, String comentario) {
        SolicitudRetiro solicitud = pendiente(idSolicitud);
        responder(solicitud, EstadosSolicitud.RECHAZADA, lider, comentario);
        return aResponse(solicitud);
    }

    private void responder(SolicitudRetiro solicitud, String estado, Usuario lider, String comentario) {
        solicitud.setEstado(buscadorEstados.deSolicitud(estado));
        solicitud.setFechaRespuesta(ZonedDateTime.now());
        solicitud.setUsuarioResponde(lider);
        solicitud.setComentarioRespuesta(comentario == null || comentario.isBlank() ? null : comentario.trim());
        solicitudRepository.save(solicitud);
        log.info("Solicitud de retiro {} {} por el usuario {}", solicitud.getId(), estado.toLowerCase(), lider.getId());
    }

    private SolicitudRetiro pendiente(Integer idSolicitud) {
        SolicitudRetiro solicitud = buscar(idSolicitud);
        if (!solicitud.estaPendiente()) {
            throw new ErrorLogicaNegocioException("La solicitud ya fue respondida.");
        }
        return solicitud;
    }

    private SolicitudRetiro buscar(Integer idSolicitud) {
        return solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de retiro", idSolicitud));
    }

    private SolicitudRetiroResponseDTO aResponse(SolicitudRetiro solicitud) {
        EstudianteSemillero vinculacion = solicitud.getVinculacion();
        return SolicitudRetiroResponseDTO.builder()
                .id(solicitud.getId())
                .idSemillero(vinculacion.getSemillero().getId())
                .nombreSemillero(vinculacion.getSemillero().getNombre())
                .idEstudiante(vinculacion.getEstudiante().getId())
                .nombreEstudiante(vinculacion.getEstudiante().getUsuario().getNombreCompleto())
                .motivo(solicitud.getMotivo())
                .estado(solicitud.getEstado().getNombre())
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .fechaRespuesta(solicitud.getFechaRespuesta())
                .nombreQuienResponde(solicitud.getUsuarioResponde() != null
                        ? solicitud.getUsuarioResponde().getNombreCompleto() : null)
                .comentarioRespuesta(solicitud.getComentarioRespuesta())
                .build();
    }
}
