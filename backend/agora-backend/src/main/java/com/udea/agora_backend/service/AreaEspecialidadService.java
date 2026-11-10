package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.AreaEspecialidadRequestDTO;
import com.udea.agora_backend.dto.response.AreaEspecialidadResponseDTO;
import com.udea.agora_backend.evento.SemillerosModificadosEvent;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.AreaEspecialidad;
import com.udea.agora_backend.repository.AreaEspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de áreas de especialidad. Cada área es una dimensión del árbol KD de semilleros
 * similares, así que crear, renombrar o eliminar una invalida ese índice.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AreaEspecialidadService {

    private final AreaEspecialidadRepository areaRepository;
    private final ApplicationEventPublisher eventos;

    @Transactional(readOnly = true)
    public List<AreaEspecialidadResponseDTO> obtenerTodas() {
        return areaRepository.findAll().stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public AreaEspecialidadResponseDTO obtenerPorId(Integer id) {
        return aResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<AreaEspecialidadResponseDTO> buscarPorNombre(String nombre) {
        return areaRepository.findByNombreContainingIgnoreCaseOrderByNombre(nombre).stream()
                .map(this::aResponse)
                .toList();
    }

    public AreaEspecialidadResponseDTO crear(AreaEspecialidadRequestDTO request) {
        AreaEspecialidad area = AreaEspecialidad.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        publicarCambioSemilleros();
        return aResponse(areaRepository.save(area));
    }

    public AreaEspecialidadResponseDTO actualizar(Integer id, AreaEspecialidadRequestDTO request) {
        AreaEspecialidad area = buscar(id);
        area.setNombre(request.getNombre());
        area.setDescripcion(request.getDescripcion());
        publicarCambioSemilleros();
        return aResponse(areaRepository.save(area));
    }

    public void eliminar(Integer id) {
        areaRepository.delete(buscar(id));
        publicarCambioSemilleros();
    }

    private AreaEspecialidad buscar(Integer id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", id));
    }

    private AreaEspecialidadResponseDTO aResponse(AreaEspecialidad area) {
        return AreaEspecialidadResponseDTO.builder()
                .id(area.getId())
                .nombre(area.getNombre())
                .descripcion(area.getDescripcion())
                .build();
    }

    // Invalida el árbol KD de semilleros similares. El índice escucha el evento después del
    // commit: si la operación falla y se revierte, el árbol no se descarta.
    private void publicarCambioSemilleros() {
        eventos.publishEvent(new SemillerosModificadosEvent());
    }
}
