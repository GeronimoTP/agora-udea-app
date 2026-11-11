package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosSolicitud;
import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import com.udea.agora_backend.dto.request.AprobacionCatalogoRequestDTO;
import com.udea.agora_backend.dto.request.AreaEspecialidadRequestDTO;
import com.udea.agora_backend.dto.request.ElementoCatalogoRequestDTO;
import com.udea.agora_backend.dto.request.HabilidadRequestDTO;
import com.udea.agora_backend.dto.request.LineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.SolicitudCatalogoRequestDTO;
import com.udea.agora_backend.dto.response.SolicitudCatalogoResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.AreaEspecialidad;
import com.udea.agora_backend.model.SolicitudCatalogo;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.AreaEspecialidadRepository;
import com.udea.agora_backend.repository.SolicitudCatalogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Solicitudes de catálogo (decisión de la reunión con el profesor). Estudiantes y profesores
 * piden agregar habilidades, líneas de investigación, áreas de especialidad, entidades
 * otorgantes o tipos de reconocimiento; el administrador evalúa si son válidas e importantes.
 * <ul>
 *   <li>Aprobada: el elemento se crea en el catálogo (el administrador puede ajustar el nombre,
 *       la descripción o el área antes) y queda disponible para todos.</li>
 *   <li>Rechazada: con un motivo obligatorio, que el solicitante ve.</li>
 * </ul>
 * No se aceptan solicitudes de algo que ya existe en el catálogo o que ya está pendiente.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SolicitudCatalogoService {

    private final SolicitudCatalogoRepository solicitudRepository;
    private final AreaEspecialidadRepository areaRepository;
    private final NombresCatalogo nombresCatalogo;
    private final BuscadorEstados buscadorEstados;
    private final HabilidadService habilidadService;
    private final LineaInvestigacionService lineaInvestigacionService;
    private final AreaEspecialidadService areaEspecialidadService;
    private final CatalogoService catalogoService;

    public SolicitudCatalogoResponseDTO crear(Usuario solicitante, SolicitudCatalogoRequestDTO request) {
        TipoElementoCatalogo tipo = request.getTipo();
        String nombre = request.getNombre().trim();
        String descripcion = request.getDescripcion() == null ? null : request.getDescripcion().trim();
        if (tipo.requiereDescripcion() && (descripcion == null || descripcion.isEmpty())) {
            throw new ErrorLogicaNegocioException("Describe brevemente " + tipo.getEtiqueta().toLowerCase() + ".");
        }
        AreaEspecialidad area = null;
        if (tipo == TipoElementoCatalogo.LINEA_INVESTIGACION) {
            if (request.getIdAreaEspecialidad() == null) {
                throw new ErrorLogicaNegocioException("Indica a qué área de especialidad pertenece la línea.");
            }
            area = buscarArea(request.getIdAreaEspecialidad());
        }
        nombresCatalogo.validarLibre(tipo, nombre);
        if (solicitudRepository.existeConNombre(tipo, nombre, EstadosSolicitud.PENDIENTE)) {
            throw new ConflictoException("Ya hay una solicitud pendiente para " + tipo.getEtiqueta().toLowerCase()
                    + " '" + nombre + "'.");
        }

        SolicitudCatalogo solicitud = solicitudRepository.save(SolicitudCatalogo.builder()
                .tipo(tipo)
                .nombre(nombre)
                .descripcion(tipo.requiereDescripcion() ? descripcion : null)
                .areaEspecialidad(area)
                .justificacion(request.getJustificacion().trim())
                .solicitante(solicitante)
                .estado(buscadorEstados.deSolicitud(EstadosSolicitud.PENDIENTE))
                .fechaSolicitud(ZonedDateTime.now())
                .build());
        log.info("Usuario {} solicitó agregar {} '{}'", solicitante.getId(), tipo, nombre);
        return aResponse(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudCatalogoResponseDTO> delUsuario(Usuario usuario) {
        return solicitudRepository.findBySolicitanteIdOrderByFechaSolicitudDesc(usuario.getId()).stream()
                .map(this::aResponse).toList();
    }

    /** Para el administrador: solo las pendientes (por orden de llegada) o todas. */
    @Transactional(readOnly = true)
    public List<SolicitudCatalogoResponseDTO> listar(boolean soloPendientes) {
        return (soloPendientes
                ? solicitudRepository.findByNombreEstado(EstadosSolicitud.PENDIENTE)
                : solicitudRepository.findTodasRecientesPrimero())
                .stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public long contarPendientes() {
        return solicitudRepository.contarPorEstado(EstadosSolicitud.PENDIENTE);
    }

    /** Crea el elemento en el catálogo con los datos de la solicitud o los ajustes del administrador. */
    public SolicitudCatalogoResponseDTO aprobar(Integer idSolicitud, Usuario administrador,
                                                AprobacionCatalogoRequestDTO ajustes) {
        SolicitudCatalogo solicitud = pendiente(idSolicitud);
        AprobacionCatalogoRequestDTO datos = ajustes != null ? ajustes : new AprobacionCatalogoRequestDTO();
        String nombre = conValor(datos.getNombre(), solicitud.getNombre());
        String descripcion = conValor(datos.getDescripcion(), solicitud.getDescripcion());
        Integer idArea = datos.getIdAreaEspecialidad() != null ? datos.getIdAreaEspecialidad()
                : solicitud.getAreaEspecialidad() != null ? solicitud.getAreaEspecialidad().getId() : null;

        solicitud.setNombre(nombre);
        solicitud.setDescripcion(descripcion);
        if (idArea != null && solicitud.getTipo() == TipoElementoCatalogo.LINEA_INVESTIGACION) {
            solicitud.setAreaEspecialidad(buscarArea(idArea));
        }
        solicitud.setIdElementoCreado(crearElemento(solicitud.getTipo(), nombre, descripcion, idArea));
        responder(solicitud, EstadosSolicitud.APROBADA, administrador, datos.getComentario());
        return aResponse(solicitud);
    }

    public SolicitudCatalogoResponseDTO rechazar(Integer idSolicitud, Usuario administrador, String motivo) {
        SolicitudCatalogo solicitud = pendiente(idSolicitud);
        responder(solicitud, EstadosSolicitud.RECHAZADA, administrador, motivo);
        return aResponse(solicitud);
    }

    // Reutiliza los servicios de cada catálogo: validan el nombre repetido e invalidan el árbol KD
    private Integer crearElemento(TipoElementoCatalogo tipo, String nombre, String descripcion, Integer idArea) {
        return switch (tipo) {
            case HABILIDAD -> {
                HabilidadRequestDTO request = new HabilidadRequestDTO();
                request.setNombre(nombre);
                yield habilidadService.crear(request).getId();
            }
            case LINEA_INVESTIGACION -> {
                if (idArea == null) {
                    throw new ErrorLogicaNegocioException("La línea necesita un área de especialidad.");
                }
                LineaInvestigacionRequestDTO request = new LineaInvestigacionRequestDTO();
                request.setNombre(nombre);
                request.setDescripcion(descripcion);
                request.setIdAreaEspecialidad(idArea);
                yield lineaInvestigacionService.crear(request).getId();
            }
            case AREA_ESPECIALIDAD -> {
                AreaEspecialidadRequestDTO request = new AreaEspecialidadRequestDTO();
                request.setNombre(nombre);
                request.setDescripcion(descripcion);
                yield areaEspecialidadService.crear(request).getId();
            }
            case ENTIDAD_OTORGANTE -> catalogoService.crearEntidadOtorgante(elemento(nombre, descripcion)).getId();
            case TIPO_RECONOCIMIENTO -> catalogoService.crearTipoReconocimiento(elemento(nombre, descripcion)).getId();
        };
    }

    private static ElementoCatalogoRequestDTO elemento(String nombre, String descripcion) {
        ElementoCatalogoRequestDTO request = new ElementoCatalogoRequestDTO();
        request.setNombre(nombre);
        request.setDescripcion(descripcion);
        return request;
    }

    private void responder(SolicitudCatalogo solicitud, String estado, Usuario administrador, String comentario) {
        solicitud.setEstado(buscadorEstados.deSolicitud(estado));
        solicitud.setFechaRespuesta(ZonedDateTime.now());
        solicitud.setUsuarioResponde(administrador);
        solicitud.setComentarioRespuesta(comentario == null || comentario.isBlank() ? null : comentario.trim());
        solicitudRepository.save(solicitud);
        log.info("Solicitud de catálogo {} {} por el usuario {}", solicitud.getId(), estado.toLowerCase(), administrador.getId());
    }

    private SolicitudCatalogo pendiente(Integer idSolicitud) {
        SolicitudCatalogo solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud de catálogo", idSolicitud));
        if (!solicitud.estaPendiente()) {
            throw new ErrorLogicaNegocioException("La solicitud ya fue respondida.");
        }
        return solicitud;
    }

    private AreaEspecialidad buscarArea(Integer idArea) {
        return areaRepository.findById(idArea)
                .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", idArea));
    }

    private static String conValor(String ajuste, String original) {
        return ajuste != null && !ajuste.isBlank() ? ajuste.trim() : original;
    }

    private SolicitudCatalogoResponseDTO aResponse(SolicitudCatalogo solicitud) {
        Usuario solicitante = solicitud.getSolicitante();
        AreaEspecialidad area = solicitud.getAreaEspecialidad();
        return SolicitudCatalogoResponseDTO.builder()
                .id(solicitud.getId())
                .tipo(solicitud.getTipo())
                .etiquetaTipo(solicitud.getTipo().getEtiqueta())
                .nombre(solicitud.getNombre())
                .descripcion(solicitud.getDescripcion())
                .idAreaEspecialidad(area != null ? area.getId() : null)
                .nombreAreaEspecialidad(area != null ? area.getNombre() : null)
                .justificacion(solicitud.getJustificacion())
                .idSolicitante(solicitante.getId())
                .nombreSolicitante(solicitante.getNombreCompleto())
                .rolSolicitante(solicitante.getRol().getNombre())
                .estado(solicitud.getEstado().getNombre())
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .fechaRespuesta(solicitud.getFechaRespuesta())
                .nombreQuienResponde(solicitud.getUsuarioResponde() != null
                        ? solicitud.getUsuarioResponde().getNombreCompleto() : null)
                .comentarioRespuesta(solicitud.getComentarioRespuesta())
                .idElementoCreado(solicitud.getIdElementoCreado())
                .build();
    }
}
