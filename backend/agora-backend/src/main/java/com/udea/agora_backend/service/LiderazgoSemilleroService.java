package com.udea.agora_backend.service;

import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Determina qué semilleros lidera un usuario: el estudiante líder del semillero
 * o un profesor vinculado a él. Las validaciones de acceso están en AutorizacionService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiderazgoSemilleroService {

    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final SemilleroRepository semilleroRepository;

    public List<Integer> obtenerSemillerosLiderados(Usuario usuario) {
        Set<Integer> ids = new LinkedHashSet<>();
        estudianteRepository.findByUsuarioId(usuario.getId())
                .ifPresent(estudiante -> ids.addAll(
                        semilleroRepository.findIdsByEstudianteLiderId(estudiante.getId())));
        profesorRepository.findByUsuarioId(usuario.getId())
                .ifPresent(profesor -> ids.addAll(
                        profesorSemilleroRepository.findSemilleroIdsByProfesorId(profesor.getId())));
        return List.copyOf(ids);
    }
}
