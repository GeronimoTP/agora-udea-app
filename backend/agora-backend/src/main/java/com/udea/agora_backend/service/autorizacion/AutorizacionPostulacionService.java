package com.udea.agora_backend.service.autorizacion;

import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.PostulacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un estudiante solo crea, edita y decide sobre sus propias postulaciones; los líderes del
 * semillero las evalúan. El administrador también puede consultarlas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutorizacionPostulacionService {

    private final UsuarioAutenticadoService usuarioAutenticado;
    private final AutorizacionSemilleroService autorizacionSemillero;
    private final PostulacionRepository postulacionRepository;

    public void validarPropietarioPostulacion(String email, Integer idPostulacion) {
        if (!esPropietario(email, obtenerPostulacion(idPostulacion))) {
            throw new AccessDeniedException("Solo el estudiante postulado puede realizar esta acción");
        }
    }

    public void validarLiderDePostulacion(String email, Integer idPostulacion) {
        autorizacionSemillero.validarLiderSemillero(email, idSemilleroDe(obtenerPostulacion(idPostulacion)));
    }

    /** Consulta de una postulación: el estudiante que la hizo, un líder del semillero o el administrador. */
    public void validarLecturaPostulacion(String email, Integer idPostulacion) {
        Postulacion postulacion = obtenerPostulacion(idPostulacion);
        Usuario usuario = usuarioAutenticado.usuario(email);
        boolean puedeLeer = esPropietario(email, postulacion)
                || autorizacionSemillero.esLider(usuario, idSemilleroDe(postulacion))
                || usuarioAutenticado.esAdministrador(usuario);
        if (!puedeLeer) {
            throw new AccessDeniedException("No tienes acceso a esta postulación");
        }
    }

    private boolean esPropietario(String email, Postulacion postulacion) {
        return usuarioAutenticado.obtenerIdEstudiante(email)
                .map(id -> id.equals(postulacion.getEstudiante().getId()))
                .orElse(false);
    }

    private static Integer idSemilleroDe(Postulacion postulacion) {
        return postulacion.getConvocatoria().getSemillero().getId();
    }

    private Postulacion obtenerPostulacion(Integer idPostulacion) {
        return postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion));
    }
}
