package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.EstudianteRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.repository.ProgramaAcademicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de estudiantes.
 * Maneja operaciones CRUD y búsquedas relacionadas con estudiantes.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProgramaAcademicoRepository programaRepository;

    /**
     * Obtiene todos los estudiantes
     */
    public List<EstudianteResponseDTO> obtenerTodos() {
        return estudianteRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un estudiante por su ID
     */
    public EstudianteResponseDTO obtenerPorId(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));
        return mapeoAResponseDTO(estudiante);
    }

    /**
     * Obtiene un estudiante por ID de usuario
     */
    public EstudianteResponseDTO obtenerPorIdUsuario(Integer idUsuario) {
        Estudiante estudiante = estudianteRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", "usuarioId", idUsuario));
        return mapeoAResponseDTO(estudiante);
    }

    /**
     * Crea un nuevo estudiante
     */
    public EstudianteResponseDTO crear(EstudianteRequestDTO request) {
        // Obtener usuario
        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getIdUsuario()));

        // Obtener programa
        ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));

        // Crear estudiante
        Estudiante estudiante = Estudiante.builder()
                .usuario(usuario)
                .programa(programa)
                .semestre(request.getSemestre())
                .createdAt(ZonedDateTime.now())
                .build();

        Estudiante estudianteGuardado = estudianteRepository.save(estudiante);
        return mapeoAResponseDTO(estudianteGuardado);
    }

    /**
     * Actualiza un estudiante existente
     */
    public EstudianteResponseDTO actualizar(Integer id, EstudianteRequestDTO request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));

        // Obtener programa si cambió
        if (!estudiante.getPrograma().getId().equals(request.getIdPrograma())) {
            ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));
            estudiante.setPrograma(programa);
        }

        // Actualizar semestre
        estudiante.setSemestre(request.getSemestre());

        Estudiante estudianteActualizado = estudianteRepository.save(estudiante);
        return mapeoAResponseDTO(estudianteActualizado);
    }

    /**
     * Elimina un estudiante por su ID
     */
    public void eliminar(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));
        estudianteRepository.delete(estudiante);
    }

    /**
     * Mapea entidad Estudiante a ResponseDTO
     */
    private EstudianteResponseDTO mapeoAResponseDTO(Estudiante estudiante) {
        return EstudianteResponseDTO.builder()
                .id(estudiante.getId())
                .nombreCompleto(estudiante.getUsuario().getNombreCompleto())
                .email(estudiante.getUsuario().getEmail())
                .nombrePrograma(estudiante.getPrograma().getNombre())
                .semestre(estudiante.getSemestre())
                .createdAt(estudiante.getCreatedAt())
                .build();
    }
}
