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

    public boolean esAdministrador(Usuario usuario) {
        return NombresRol.ADMINISTRADOR.equals(usuario.getRol().getNombre());
    }
}
