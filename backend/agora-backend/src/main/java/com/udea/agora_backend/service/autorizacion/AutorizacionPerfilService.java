package com.udea.agora_backend.service.autorizacion;

import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.service.LiderazgoSemilleroService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Perfiles de estudiante y profesor. Cada uno gestiona solo el suyo; para ver el de otro hay
 * que tener un motivo: el administrador, o el líder de un semillero al que el estudiante
 * pertenece, perteneció o se postuló. El correo y los datos personales no se exponen a
 * cualquier usuario con sesión (antes bastaba cambiar el ID en la URL).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionPerfilService {

    private final UsuarioAutenticadoService usuarioAutenticado;
    private final LiderazgoSemilleroService liderazgoSemilleroService;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final PostulacionRepository postulacionRepository;

    public void validarEstudianteActual(String email, Integer idEstudiante) {
        if (!usuarioAutenticado.obtenerIdEstudiante(email).map(id -> id.equals(idEstudiante)).orElse(false)) {
            throw new AccessDeniedException("Solo puedes gestionar tu propio perfil de estudiante");
        }
    }

    public void validarProfesorActual(String email, Integer idProfesor) {
        if (!usuarioAutenticado.obtenerIdProfesor(email).map(id -> id.equals(idProfesor)).orElse(false)) {
            throw new AccessDeniedException("Solo puedes gestionar tu propio perfil de profesor");
        }
    }

    /** Ver el perfil completo de un estudiante: él mismo, el administrador o un líder de uno de sus semilleros. */
    public void validarLecturaEstudiante(String email, Integer idEstudiante) {
        Usuario usuario = usuarioAutenticado.usuario(email);
        boolean propio = usuarioAutenticado.obtenerIdEstudiante(email).map(idEstudiante::equals).orElse(false);
        if (propio || usuarioAutenticado.esAdministrador(usuario)) {
            return;
        }
        // Semilleros en los que está o estuvo, y a los que se postuló
        Set<Integer> suyos = new HashSet<>();
        estudianteSemilleroRepository.findByEstudianteId(idEstudiante)
                .forEach(v -> suyos.add(v.getSemillero().getId()));
        postulacionRepository.findByEstudianteId(idEstudiante)
                .forEach(p -> suyos.add(p.getConvocatoria().getSemillero().getId()));
        if (liderazgoSemilleroService.obtenerSemillerosLiderados(usuario).stream().noneMatch(suyos::contains)) {
            throw new AccessDeniedException("No tienes acceso al perfil de este estudiante");
        }
    }

    /**
     * Listado de estudiantes (sin correo): lo usan el profesor que registra un semillero para
     * elegir al líder y los líderes. Un estudiante sin semillero liderado no lo necesita.
     */
    public void validarListadoEstudiantes(String email) {
        Usuario usuario = usuarioAutenticado.usuario(email);
        boolean permitido = usuarioAutenticado.esAdministrador(usuario)
                || usuarioAutenticado.obtenerIdProfesor(email).isPresent()
                || !liderazgoSemilleroService.obtenerSemillerosLiderados(usuario).isEmpty();
        if (!permitido) {
            throw new AccessDeniedException("Solo profesores y líderes de semillero pueden consultar el listado de estudiantes");
        }
    }

    /** Ver el perfil completo (con correo) de un profesor: él mismo o el administrador. */
    public void validarLecturaProfesor(String email, Integer idProfesor) {
        boolean propio = usuarioAutenticado.obtenerIdProfesor(email).map(idProfesor::equals).orElse(false);
        if (!propio && !usuarioAutenticado.esAdministrador(usuarioAutenticado.usuario(email))) {
            throw new AccessDeniedException("No tienes acceso al perfil de este profesor");
        }
    }
}
