package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ProyectoRequestDTO;
import com.udea.agora_backend.dto.response.ProyectoResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de proyectos de investigación.
 * Maneja operaciones CRUD y búsquedas relacionadas con proyectos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final SemilleroRepository semilleroRepository;

    /**
     * Obtiene todos los proyectos
     */
    public List<ProyectoResponseDTO> obtenerTodos() {
        return proyectoRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un proyecto por su ID
     */
    public ProyectoResponseDTO obtenerPorId(Integer id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
        return mapeoAResponseDTO(proyecto);
    }

    /**
     * Obtiene proyectos de un semillero
     */
    public List<ProyectoResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return proyectoRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo proyecto
     */
    public ProyectoResponseDTO crear(ProyectoRequestDTO request) {
        // Obtener semillero
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        // Crear proyecto
        Proyecto proyecto = Proyecto.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .semillero(semillero)
                .createdAt(ZonedDateTime.now())
                .build();

        Proyecto proyectoGuardado = proyectoRepository.save(proyecto);
        return mapeoAResponseDTO(proyectoGuardado);
    }

    /**
     * Actualiza un proyecto existente
     */
    public ProyectoResponseDTO actualizar(Integer id, ProyectoRequestDTO request) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));

        // Obtener semillero si cambió
        if (!proyecto.getSemillero().getId().equals(request.getIdSemillero())) {
            Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));
            proyecto.setSemillero(semillero);
        }

        // Actualizar campos
        proyecto.setTitulo(request.getTitulo());
        proyecto.setDescripcion(request.getDescripcion());

        Proyecto proyectoActualizado = proyectoRepository.save(proyecto);
        return mapeoAResponseDTO(proyectoActualizado);
    }

    /**
     * Elimina un proyecto por su ID
     */
    public void eliminar(Integer id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
        proyectoRepository.delete(proyecto);
    }

    /**
     * Mapea entidad Proyecto a ResponseDTO
     */
    private ProyectoResponseDTO mapeoAResponseDTO(Proyecto proyecto) {
        return ProyectoResponseDTO.builder()
                .id(proyecto.getId())
                .titulo(proyecto.getTitulo())
                .descripcion(proyecto.getDescripcion())
                .nombreSemillero(proyecto.getSemillero().getNombre())
                .build();
    }
}
