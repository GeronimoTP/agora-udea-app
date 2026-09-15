package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ProfesorAreaEspecialidadRequestDTO;
import com.udea.agora_backend.dto.request.ProfesorRequestDTO;
import com.udea.agora_backend.dto.response.ProfesorAreaEspecialidadResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de profesores.
 * Maneja operaciones CRUD, búsquedas y asignación de áreas de especialidad.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final AreaEspecialidadRepository areaEspecialidadRepository;
    private final ProfesorAreaEspecialidadRepository profesorAreaRepository;

    /**
     * Obtiene todos los profesores
     */
    public List<ProfesorResponseDTO> obtenerTodos() {
        return profesorRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un profesor por su ID
     */
    public ProfesorResponseDTO obtenerPorId(Integer id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", id));
        return mapeoAResponseDTO(profesor);
    }

    /**
     * Obtiene un profesor por ID de usuario
     */
    public ProfesorResponseDTO obtenerPorIdUsuario(Integer idUsuario) {
        Profesor profesor = profesorRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", "usuarioId", idUsuario));
        return mapeoAResponseDTO(profesor);
    }

    /**
     * Crea un nuevo profesor
     */
    public ProfesorResponseDTO crear(ProfesorRequestDTO request) {
        // Verificar usuario no duplicado
        if (profesorRepository.findByUsuarioId(request.getIdUsuario()).isPresent()) {
            throw new ConflictoException("Profesor", "usuarioId", request.getIdUsuario().toString());
        }

        // Obtener usuario
        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getIdUsuario()));

        // Obtener programa
        ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));

        // Crear profesor
        Profesor profesor = Profesor.builder()
                .usuario(usuario)
                .programa(programa)
                .createdAt(ZonedDateTime.now())
                .build();

        Profesor profesorGuardado = profesorRepository.save(profesor);

        // Guardar áreas de especialidad iniciales
        if (request.getIdAreasEspecialidad() != null && !request.getIdAreasEspecialidad().isEmpty()) {
            for (Integer idArea : request.getIdAreasEspecialidad()) {
                AreaEspecialidad area = areaEspecialidadRepository.findById(idArea)
                        .orElseThrow(() -> new RecursoNoEncontradoException("AreaEspecialidad", idArea));
                
                ProfesorAreaEspecialidad pae = ProfesorAreaEspecialidad.builder()
                        .profesor(profesorGuardado)
                        .areaEspecialidad(area)
                        .build();
                profesorAreaRepository.save(pae);
            }
        }

        return mapeoAResponseDTO(profesorGuardado);
    }

    /**
     * Actualiza un profesor existente
     */
    public ProfesorResponseDTO actualizar(Integer id, ProfesorRequestDTO request) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", id));

        // Obtener programa si cambió
        if (!profesor.getPrograma().getId().equals(request.getIdPrograma())) {
            ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));
            profesor.setPrograma(programa);
        }

        Profesor profesorActualizado = profesorRepository.save(profesor);

        // Sincronizar áreas de especialidad si vienen en el request
        if (request.getIdAreasEspecialidad() != null) {
            // Eliminar anteriores
            List<ProfesorAreaEspecialidad> anteriores = profesorAreaRepository.findByProfesorId(id);
            profesorAreaRepository.deleteAll(anteriores);

            // Guardar nuevas
            for (Integer idArea : request.getIdAreasEspecialidad()) {
                AreaEspecialidad area = areaEspecialidadRepository.findById(idArea)
                        .orElseThrow(() -> new RecursoNoEncontradoException("AreaEspecialidad", idArea));
                
                ProfesorAreaEspecialidad pae = ProfesorAreaEspecialidad.builder()
                        .profesor(profesorActualizado)
                        .areaEspecialidad(area)
                        .build();
                profesorAreaRepository.save(pae);
            }
        }

        return mapeoAResponseDTO(profesorActualizado);
    }

    /**
     * Elimina un profesor por su ID
     */
    public void eliminar(Integer id) {
        Profesor profesor = profesorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", id));
        profesorRepository.delete(profesor);
    }

    // ==========================================
    // ÁREAS DE ESPECIALIDAD
    // ==========================================

    public ProfesorAreaEspecialidadResponseDTO agregarAreaEspecialidad(ProfesorAreaEspecialidadRequestDTO request) {
        Profesor profesor = profesorRepository.findById(request.getIdProfesor())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", request.getIdProfesor()));

        AreaEspecialidad area = areaEspecialidadRepository.findById(request.getIdAreaEspecialidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("AreaEspecialidad", request.getIdAreaEspecialidad()));

        if (profesorAreaRepository.findByProfesorIdAndAreaEspecialidadId(request.getIdProfesor(), request.getIdAreaEspecialidad()).isPresent()) {
            throw new ConflictoException("ProfesorAreaEspecialidad", "idAreaEspecialidad", request.getIdAreaEspecialidad().toString());
        }

        ProfesorAreaEspecialidad asociacion = ProfesorAreaEspecialidad.builder()
                .profesor(profesor)
                .areaEspecialidad(area)
                .build();

        ProfesorAreaEspecialidad guardada = profesorAreaRepository.save(asociacion);

        return ProfesorAreaEspecialidadResponseDTO.builder()
                .id(guardada.getId())
                .nombreProfesor(profesor.getUsuario().getNombreCompleto())
                .nombreAreaEspecialidad(area.getNombre())
                .build();
    }

    public List<ProfesorAreaEspecialidadResponseDTO> obtenerAreasEspecialidad(Integer idProfesor) {
        return profesorAreaRepository.findByProfesorId(idProfesor)
                .stream()
                .map(pae -> ProfesorAreaEspecialidadResponseDTO.builder()
                        .id(pae.getId())
                        .nombreProfesor(pae.getProfesor().getUsuario().getNombreCompleto())
                        .nombreAreaEspecialidad(pae.getAreaEspecialidad().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public void eliminarAreaEspecialidad(Integer idProfesor, Integer idArea) {
        ProfesorAreaEspecialidad asociacion = profesorAreaRepository.findByProfesorIdAndAreaEspecialidadId(idProfesor, idArea)
                .orElseThrow(() -> new RecursoNoEncontradoException("ProfesorAreaEspecialidad", "idArea", idArea));
        profesorAreaRepository.delete(asociacion);
    }

    /**
     * Mapea entidad Profesor a ResponseDTO
     */
    private ProfesorResponseDTO mapeoAResponseDTO(Profesor profesor) {
        List<String> areas = profesorAreaRepository.findByProfesorId(profesor.getId())
                .stream()
                .map(pae -> pae.getAreaEspecialidad().getNombre())
                .collect(Collectors.toList());

        return ProfesorResponseDTO.builder()
                .id(profesor.getId())
                .nombreCompleto(profesor.getUsuario() != null ? profesor.getUsuario().getNombreCompleto() : null)
                .email(profesor.getUsuario() != null ? profesor.getUsuario().getEmail() : null)
                .nombrePrograma(profesor.getPrograma() != null ? profesor.getPrograma().getNombre() : null)
                .areasEspecialidad(areas)
                .createdAt(profesor.getCreatedAt())
                .build();
    }
}
