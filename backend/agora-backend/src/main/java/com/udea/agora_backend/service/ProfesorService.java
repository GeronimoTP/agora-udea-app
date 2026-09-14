package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ProfesorRequestDTO;
import com.udea.agora_backend.dto.response.ProfesorResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Profesor;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.repository.ProgramaAcademicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de profesores.
 * Maneja operaciones CRUD y búsquedas relacionadas con profesores.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfesorService {

    private final ProfesorRepository profesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProgramaAcademicoRepository programaRepository;

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

    /**
     * Mapea entidad Profesor a ResponseDTO
     */
    private ProfesorResponseDTO mapeoAResponseDTO(Profesor profesor) {
        return ProfesorResponseDTO.builder()
                .id(profesor.getId())
                .nombreCompleto(profesor.getUsuario().getNombreCompleto())
                .email(profesor.getUsuario().getEmail())
                .nombrePrograma(profesor.getPrograma().getNombre())
                .createdAt(profesor.getCreatedAt())
                .build();
    }
}
