package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.EstadoRequestDTO;
import com.udea.agora_backend.dto.response.EstadoResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.CategoriaEstado;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.CategoriaEstadoRepository;
import com.udea.agora_backend.repository.EstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de estados (solo el administrador lo modifica). Los servicios de negocio no lo
 * usan para buscar estados: para eso está {@link BuscadorEstados}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EstadoService {

    private final EstadoRepository estadoRepository;
    private final CategoriaEstadoRepository categoriaEstadoRepository;

    @Transactional(readOnly = true)
    public List<EstadoResponseDTO> obtenerTodos() {
        return estadoRepository.findAll().stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public EstadoResponseDTO obtenerPorId(Integer id) {
        return aResponse(buscar(id));
    }

    /** Los estados con ese nombre (puede haber uno por categoría). */
    @Transactional(readOnly = true)
    public List<EstadoResponseDTO> obtenerPorNombre(String nombre) {
        List<Estado> estados = estadoRepository.findByNombreIgnoreCase(nombre);
        if (estados.isEmpty()) {
            throw new RecursoNoEncontradoException("Estado", "nombre", nombre);
        }
        return estados.stream().map(this::aResponse).toList();
    }

    /** Los estados de una categoría (ver CategoriasEstado). */
    @Transactional(readOnly = true)
    public List<EstadoResponseDTO> obtenerPorCategoria(String categoria) {
        return estadoRepository.findByCategoriaEstadoNombreIgnoreCase(categoria).stream()
                .map(this::aResponse)
                .toList();
    }

    public EstadoResponseDTO crear(EstadoRequestDTO request) {
        CategoriaEstado categoria = categoriaEstadoRepository.findById(request.getIdCategoriaEstado())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría de Estado", request.getIdCategoriaEstado()));
        Estado estado = Estado.builder()
                .categoriaEstado(categoria)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .build();
        return aResponse(estadoRepository.save(estado));
    }

    /** Cambia nombre y descripción; la categoría de un estado no cambia. */
    public EstadoResponseDTO actualizar(Integer id, EstadoRequestDTO request) {
        Estado estado = buscar(id);
        estado.setNombre(request.getNombre());
        estado.setDescripcion(request.getDescripcion());
        return aResponse(estadoRepository.save(estado));
    }

    public void eliminar(Integer id) {
        estadoRepository.delete(buscar(id));
    }

    private Estado buscar(Integer id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", id));
    }

    private EstadoResponseDTO aResponse(Estado estado) {
        return EstadoResponseDTO.builder()
                .id(estado.getId())
                .nombre(estado.getNombre())
                .descripcion(estado.getDescripcion())
                .nombreCategoria(estado.getCategoriaEstado().getNombre())
                .build();
    }
}
