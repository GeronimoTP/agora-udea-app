package com.udea.agora_backend.service.autorizacion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Un estudiante o un profesor solo gestiona su propio perfil. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionPerfilService {

    private final UsuarioAutenticadoService usuarioAutenticado;

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
}
