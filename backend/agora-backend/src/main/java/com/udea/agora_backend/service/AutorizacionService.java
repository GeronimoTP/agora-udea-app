package com.udea.agora_backend.service;

import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Reglas de propiedad que complementan las reglas por rol de SecurityConfig:
 * - Un estudiante o profesor solo gestiona su propio perfil.
 * - Un estudiante solo crea, edita y decide sobre sus propias postulaciones.
 * - Solo un líder del semillero (estudiante líder o profesor vinculado) gestiona el
 *   semillero, sus convocatorias y evalúa sus postulaciones.
 * Todas reciben el email del usuario autenticado (Authentication#getName()).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionService {

    private static final String ROL_ADMINISTRADOR = "Administrador";

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final PostulacionRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final LiderazgoSemilleroService liderazgoSemilleroService;

    // ==========================================
    // PERFILES
    // ==========================================

    public Optional<Integer> obtenerIdEstudiante(String email) {
        return estudianteRepository.findByUsuarioId(usuarioActual(email).getId()).map(e -> e.getId());
    }

    public Optional<Integer> obtenerIdProfesor(String email) {
        return profesorRepository.findByUsuarioId(usuarioActual(email).getId()).map(p -> p.getId());
    }

    public void validarEstudianteActual(String email, Integer idEstudiante) {
        if (!obtenerIdEstudiante(email).map(id -> id.equals(idEstudiante)).orElse(false)) {
            throw new AccessDeniedException("Solo puedes gestionar tu propio perfil de estudiante");
        }
    }

    public void validarProfesorActual(String email, Integer idProfesor) {
        if (!obtenerIdProfesor(email).map(id -> id.equals(idProfesor)).orElse(false)) {
            throw new AccessDeniedException("Solo puedes gestionar tu propio perfil de profesor");
        }
    }

    // ==========================================
    // LIDERAZGO DE SEMILLEROS Y CONVOCATORIAS
    // ==========================================

    public void validarLiderSemillero(String email, Integer idSemillero) {
        if (!liderazgoSemilleroService.obtenerSemillerosLiderados(usuarioActual(email)).contains(idSemillero)) {
            throw new AccessDeniedException("Solo un líder del semillero puede realizar esta acción");
        }
    }

    public void validarLiderConvocatoria(String email, Integer idConvocatoria) {
        Convocatoria convocatoria = convocatoriaRepository.findById(idConvocatoria)
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", idConvocatoria));
        validarLiderSemillero(email, convocatoria.getSemillero().getId());
    }

    // ==========================================
    // POSTULACIONES
    // ==========================================

    public void validarPropietarioPostulacion(String email, Integer idPostulacion) {
        if (!esPropietario(email, obtenerPostulacion(idPostulacion))) {
            throw new AccessDeniedException("Solo el estudiante postulado puede realizar esta acción");
        }
    }

    public void validarLiderDePostulacion(String email, Integer idPostulacion) {
        validarLiderSemillero(email, obtenerPostulacion(idPostulacion).getConvocatoria().getSemillero().getId());
    }

    // Consulta de una postulación: el estudiante que la hizo, un líder del semillero o el administrador
    public void validarLecturaPostulacion(String email, Integer idPostulacion) {
        Postulacion postulacion = obtenerPostulacion(idPostulacion);
        Usuario usuario = usuarioActual(email);
        boolean esLider = liderazgoSemilleroService.obtenerSemillerosLiderados(usuario)
                .contains(postulacion.getConvocatoria().getSemillero().getId());
        if (!esPropietario(email, postulacion) && !esLider && !esAdministrador(usuario)) {
            throw new AccessDeniedException("No tienes acceso a esta postulación");
        }
    }

    private boolean esPropietario(String email, Postulacion postulacion) {
        return obtenerIdEstudiante(email).map(id -> id.equals(postulacion.getEstudiante().getId())).orElse(false);
    }

    private Postulacion obtenerPostulacion(Integer idPostulacion) {
        return postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion));
    }

    private boolean esAdministrador(Usuario usuario) {
        return ROL_ADMINISTRADOR.equals(usuario.getRol().getNombre());
    }

    private Usuario usuarioActual(String email) {
        return usuarioRepository.findWithRolByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", "email", email));
    }
}
