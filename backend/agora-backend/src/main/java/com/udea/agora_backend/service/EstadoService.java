package com.udea.agora_backend.service;

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
     * Obtiene un estado por su nombre
     */
    public Estado obtenerPorNombre(String nombre) {
        return estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", nombre));
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
