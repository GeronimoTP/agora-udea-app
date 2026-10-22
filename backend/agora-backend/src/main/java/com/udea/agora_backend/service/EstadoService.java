package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.response.EstadoResponseDTO;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.EstadoRepository;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de gestión de estados.
 * Maneja operaciones CRUD simples de estados del sistema.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EstadoService {

    private final EstadoRepository estadoRepository;

    /**
     * Obtiene todos los estados
     */
    public List<Estado> obtenerTodos() {
        return estadoRepository.findAll();
    }

    /**
     * Obtiene un estado por su ID
     */
    public Estado obtenerPorId(Integer id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", id));
    }

    /**
     * Obtiene los estados con ese nombre (puede haber uno por categoría)
     */
    public List<Estado> obtenerPorNombre(String nombre) {
        List<Estado> estados = estadoRepository.findByNombreIgnoreCase(nombre);
        if (estados.isEmpty()) {
            throw new RecursoNoEncontradoException("Estado", "nombre", nombre);
        }
        return estados;
    }

    /**
     * Obtiene los estados de una categoría (ver CategoriasEstado)
     */
    public List<EstadoResponseDTO> obtenerPorCategoria(String categoria) {
        return estadoRepository.findByCategoriaEstadoNombreIgnoreCase(categoria).stream()
                .map(e -> EstadoResponseDTO.builder()
                        .id(e.getId())
                        .nombre(e.getNombre())
                        .descripcion(e.getDescripcion())
                        .nombreCategoria(e.getCategoriaEstado().getNombre())
                        .build())
                .toList();
    }

    /**
     * Obtiene el estado de una categoría (ver CategoriasEstado) por su nombre
     */
    public Estado obtener(String categoria, String nombre) {
        return estadoRepository.findByCategoriaEstadoNombreIgnoreCaseAndNombreIgnoreCase(categoria, nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", categoria + " / " + nombre));
    }

    /**
     * Crea un nuevo estado
     */
    public Estado crear(Estado estado) {
        return estadoRepository.save(estado);
    }

    /**
     * Actualiza un estado existente
     */
    public Estado actualizar(Integer id, Estado estadoActualizado) {
        Estado estado = estadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", id));
        
        // Solo se actualiza nombre y descripción, no la categoría
        estado.setNombre(estadoActualizado.getNombre());
        estado.setDescripcion(estadoActualizado.getDescripcion());

        return estadoRepository.save(estado);
    }

    /**
     * Elimina un estado por su ID
     */
    public void eliminar(Integer id) {
        Estado estado = estadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", id));
        estadoRepository.delete(estado);
    }
}
