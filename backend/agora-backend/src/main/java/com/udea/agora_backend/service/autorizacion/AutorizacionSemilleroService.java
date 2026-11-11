package com.udea.agora_backend.service.autorizacion;

import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Publicacion;
import com.udea.agora_backend.model.ReconocimientoSemillero;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.PublicacionRepository;
import com.udea.agora_backend.repository.ReconocimientoSemilleroRepository;
import com.udea.agora_backend.service.LiderazgoSemilleroService;
import com.udea.agora_backend.service.VisibilidadSemilleros;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Solo un líder del semillero (estudiante líder o profesor tutor) gestiona el semillero y su
 * contenido: convocatorias, proyectos, publicaciones y reconocimientos. Los integrantes activos
 * pueden ver a sus compañeros.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionSemilleroService {

    private final UsuarioAutenticadoService usuarioAutenticado;
    private final LiderazgoSemilleroService liderazgoSemilleroService;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final ProyectoRepository proyectoRepository;
    private final PublicacionRepository publicacionRepository;
    private final ReconocimientoSemilleroRepository reconocimientoRepository;

    /** Semilleros no aprobados que puede ver quien hace la petición (ver {@link VisibilidadSemilleros}). */
    public VisibilidadSemilleros visibilidad(String email) {
        Usuario usuario = usuarioAutenticado.usuario(email);
        return new VisibilidadSemilleros(usuarioAutenticado.esAdministrador(usuario),
                Set.copyOf(liderazgoSemilleroService.obtenerSemillerosLiderados(usuario)));
    }

    public boolean esLider(Usuario usuario, Integer idSemillero) {
        return liderazgoSemilleroService.obtenerSemillerosLiderados(usuario).contains(idSemillero);
    }

    public void validarLiderSemillero(String email, Integer idSemillero) {
        if (!esLider(usuarioAutenticado.usuario(email), idSemillero)) {
            throw new AccessDeniedException("Solo un líder del semillero puede realizar esta acción");
        }
    }

    /** Líderes del semillero (estudiante líder o tutores) y el administrador ven el historial completo de integrantes. */
    public boolean esLiderOAdministrador(String email, Integer idSemillero) {
        Usuario usuario = usuarioAutenticado.usuario(email);
        return usuarioAutenticado.esAdministrador(usuario) || esLider(usuario, idSemillero);
    }

    public void validarLiderOAdministrador(String email, Integer idSemillero) {
        if (!esLiderOAdministrador(email, idSemillero)) {
            throw new AccessDeniedException("Solo los líderes y tutores del semillero pueden ver esta información");
        }
    }

    public boolean esIntegranteActivo(String email, Integer idSemillero) {
        return usuarioAutenticado.obtenerIdEstudiante(email)
                .map(idEstudiante -> estudianteSemilleroRepository
                        .existsBySemilleroIdAndEstudianteIdAndFechaSalidaIsNull(idSemillero, idEstudiante))
                .orElse(false);
    }

    /** Un integrante activo puede ver a sus compañeros (no a los retirados). */
    public void validarIntegranteActivo(String email, Integer idSemillero) {
        if (!esIntegranteActivo(email, idSemillero)) {
            throw new AccessDeniedException("Solo los integrantes y líderes del semillero pueden ver sus integrantes");
        }
    }

    /** Líderes, tutores, integrantes activos y administrador: quienes ven datos de contacto del equipo. */
    public boolean perteneceOAdministra(String email, Integer idSemillero) {
        return esLiderOAdministrador(email, idSemillero) || esIntegranteActivo(email, idSemillero);
    }

    /** Integrantes de un proyecto: los ve quien pertenece a su semillero o lo administra. */
    public void validarLecturaIntegrantesProyecto(String email, Integer idProyecto) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", idProyecto));
        if (!perteneceOAdministra(email, proyecto.getSemillero().getId())) {
            throw new AccessDeniedException("Solo los integrantes y líderes del semillero pueden ver quién trabaja en el proyecto");
        }
    }

    public void validarLiderConvocatoria(String email, Integer idConvocatoria) {
        Convocatoria convocatoria = convocatoriaRepository.findById(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));
        validarLiderSemillero(email, convocatoria.getSemillero().getId());
    }

    public void validarLiderProyecto(String email, Integer idProyecto) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", idProyecto));
        validarLiderSemillero(email, proyecto.getSemillero().getId());
    }

    public void validarLiderPublicacion(String email, Integer idPublicacion) {
        Publicacion publicacion = publicacionRepository.findById(idPublicacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicación", idPublicacion));
        validarLiderSemillero(email, publicacion.getProyecto().getSemillero().getId());
    }

    public void validarLiderReconocimiento(String email, Integer idReconocimiento) {
        ReconocimientoSemillero reconocimiento = reconocimientoRepository.findById(idReconocimiento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reconocimiento", idReconocimiento));
        validarLiderSemillero(email, reconocimiento.getSemillero().getId());
    }
}
