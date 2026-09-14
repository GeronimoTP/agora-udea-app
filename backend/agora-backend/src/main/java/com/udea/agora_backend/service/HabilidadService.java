package com.udea.agora_backend.service;

import com.udea.agora_backend.model.Habilidad;
import com.udea.agora_backend.repository.HabilidadRepository;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de gestión de habilidades.
 * Maneja operaciones CRUD simples de habilidades.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HabilidadService {

    private final HabilidadRepository habilidadRepository;

    /**
     * Obtiene todas las habilidades
     */
    public List<Habilidad> obtenerTodas() {
        return habilidadRepository.findAll();
    }

    /**
     * Obtiene una habilidad por su ID
     */
    public Habilidad obtenerPorId(Integer id) {
        return habilidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", id));
    }

    /**
     * Obtiene habilidades por nombre (búsqueda parcial)
     */
    public List<Habilidad> obtenerPorNombreContiene(String nombre) {
        return habilidadRepository.findAll().stream()
                .filter(h -> h.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();
    }

    /**
     * Crea una nueva habilidad
     */
    public Habilidad crear(Habilidad habilidad) {
        return habilidadRepository.save(habilidad);
    }

    /**
     * Actualiza una habilidad existente
     */
    public Habilidad actualizar(Integer id, Habilidad habilidadActualizada) {
        Habilidad habilidad = habilidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", id));
        
        habilidad.setNombre(habilidadActualizada.getNombre());

        return habilidadRepository.save(habilidad);
    }

    /**
     * Elimina una habilidad por su ID
     */
    public void eliminar(Integer id) {
        Habilidad habilidad = habilidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", id));
        habilidadRepository.delete(habilidad);
    }
}
