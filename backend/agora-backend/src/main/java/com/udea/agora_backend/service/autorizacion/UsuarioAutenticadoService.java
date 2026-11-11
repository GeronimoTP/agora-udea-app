package com.udea.agora_backend.service.autorizacion;

import com.udea.agora_backend.dominio.NombresRol;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Profesor;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Identidad del usuario autenticado a partir de su email ({@code Authentication#getName()}):
 * su cuenta, su rol y sus perfiles de estudiante o profesor. La usan las reglas de autorización.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;

    public Usuario usuario(String email) {
        return usuarioRepository.findWithRolByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", "email", email));
    }

    public Optional<Integer> obtenerIdEstudiante(String email) {
        return estudianteRepository.findByUsuarioId(usuario(email).getId()).map(Estudiante::getId);
    }

    public Optional<Integer> obtenerIdProfesor(String email) {
        return profesorRepository.findByUsuarioId(usuario(email).getId()).map(Profesor::getId);
    }

    /** Rutas "/me" y "/mias": el estudiante sale del token, nunca de un ID en la URL. */
    public Integer idEstudianteActual(String email) {
        return obtenerIdEstudiante(email)
                .orElseThrow(() -> new AccessDeniedException("Esta acción es solo para estudiantes"));
    }

    public Integer idProfesorActual(String email) {
        return obtenerIdProfesor(email)
                .orElseThrow(() -> new AccessDeniedException("Esta acción es solo para profesores"));
    }

    public boolean esAdministrador(Usuario usuario) {
        return NombresRol.ADMINISTRADOR.equals(usuario.getRol().getNombre());
    }
}
