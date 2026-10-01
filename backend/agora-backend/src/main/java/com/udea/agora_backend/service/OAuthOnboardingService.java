package com.udea.agora_backend.service;

import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthOnboardingService {

    private static final String ROL_PENDIENTE = "Pendiente";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final AreaEspecialidadRepository areaEspecialidadRepository;
    private final ProfesorAreaEspecialidadRepository profesorAreaRepository;

    @Transactional
    public Integer completarEstudiante(String email, Integer idPrograma, Integer semestre,
                                       List<Integer> idLineasInvestigacion) {
        Usuario usuario = obtenerUsuarioPendiente(email);
        validarSinPerfil(usuario);

        Rol rol = obtenerRol("Estudiante");
        ProgramaAcademico programa = programaRepository.findById(idPrograma)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", idPrograma));

        Estudiante estudiante = estudianteRepository.save(Estudiante.builder()
                .usuario(usuario)
                .programa(programa)
                .semestre(semestre)
                .createdAt(ZonedDateTime.now())
                .build());

        for (Integer idLinea : idLineasInvestigacion) {
            LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Línea de Investigación", idLinea));
            estudianteLineaRepository.save(EstudianteLineaInvestigacion.builder()
                    .estudiante(estudiante)
                    .lineaInvestigacion(linea)
                    .build());
        }

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        return estudiante.getId();
    }

    @Transactional
    public Integer completarProfesor(String email, Integer idPrograma, List<Integer> idAreasEspecialidad) {
        Usuario usuario = obtenerUsuarioPendiente(email);
        validarSinPerfil(usuario);

        Rol rol = obtenerRol("Profesor");
        ProgramaAcademico programa = programaRepository.findById(idPrograma)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", idPrograma));

        Profesor profesor = profesorRepository.save(Profesor.builder()
                .usuario(usuario)
                .programa(programa)
                .createdAt(ZonedDateTime.now())
                .build());

        for (Integer idArea : idAreasEspecialidad) {
            AreaEspecialidad area = areaEspecialidadRepository.findById(idArea)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", idArea));
            profesorAreaRepository.save(ProfesorAreaEspecialidad.builder()
                    .profesor(profesor)
                    .areaEspecialidad(area)
                    .build());
        }

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        return profesor.getId();
    }

    private Usuario obtenerUsuarioPendiente(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", "email", email));

        if (!ROL_PENDIENTE.equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfil de este usuario ya fue completado");
        }
        return usuario;
    }

    private void validarSinPerfil(Usuario usuario) {
        if (estudianteRepository.findByUsuarioId(usuario.getId()).isPresent()
                || profesorRepository.findByUsuarioId(usuario.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya tiene un perfil asociado");
        }
    }

    private Rol obtenerRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", "nombre", nombre));
    }
}