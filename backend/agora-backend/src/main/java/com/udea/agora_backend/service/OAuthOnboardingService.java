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

    private final UsuarioRepository usuarioRepository;
        private final OauthProveedorRepository oauthProveedorRepository;
    private final RolRepository rolRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final AreaEspecialidadRepository areaEspecialidadRepository;
    private final ProfesorAreaEspecialidadRepository profesorAreaRepository;

    @Transactional
        public Integer completarEstudiante(String email, String oauthId, String nombreCompleto,
                                                                           Integer idPrograma, Integer semestre,
                                       List<Integer> idLineasInvestigacion) {
                OauthProveedor proveedor = obtenerProveedorGoogle();
        Rol rol = obtenerRol("Estudiante");
        ProgramaAcademico programa = programaRepository.findById(idPrograma)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", idPrograma));
                Usuario usuario = obtenerUsuarioParaCompletar(email, oauthId, nombreCompleto, proveedor, rol);

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

        return estudiante.getId();
    }

    @Transactional
        public Integer completarProfesor(String email, String oauthId, String nombreCompleto,
                                                                          Integer idPrograma, List<Integer> idAreasEspecialidad) {
                OauthProveedor proveedor = obtenerProveedorGoogle();
        Rol rol = obtenerRol("Profesor");
        ProgramaAcademico programa = programaRepository.findById(idPrograma)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", idPrograma));
                Usuario usuario = obtenerUsuarioParaCompletar(email, oauthId, nombreCompleto, proveedor, rol);

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

        return profesor.getId();
    }

        private Usuario obtenerUsuarioParaCompletar(String email, String oauthId, String nombreCompleto,
                                                                                                 OauthProveedor proveedor, Rol rol) {
                Usuario usuario = usuarioRepository.findByOauthId(oauthId).orElse(null);

                if (usuario != null) {
                        if (!usuario.getEmail().equalsIgnoreCase(email)) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                                "La cuenta Google no coincide con el correo registrado");
                        }
                        validarSinPerfil(usuario);
                } else {
                        if (usuarioRepository.findByEmail(email).isPresent()) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                                "El correo ya está asociado a otra cuenta OAuth");
                        }
                        usuario = new Usuario();
                        usuario.setOauthId(oauthId);
                        usuario.setCreatedAt(ZonedDateTime.now());
        }

                usuario.setEmail(email);
                usuario.setNombreCompleto(nombreCompleto == null || nombreCompleto.isBlank() ? email : nombreCompleto);
                usuario.setProveedorOauth(proveedor);
                usuario.setRol(rol);
                usuario = usuarioRepository.save(usuario);
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

        private OauthProveedor obtenerProveedorGoogle() {
                return oauthProveedorRepository.findByNombre("Google")
                                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor OAuth", "nombre", "Google"));
        }
}