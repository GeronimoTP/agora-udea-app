package com.udea.agora_backend.service;

import com.udea.agora_backend.model.AreaEspecialidad;
import com.udea.agora_backend.repository.AreaEspecialidadRepository;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de gestión de áreas de especialidad.
 * Maneja operaciones CRUD simples de áreas de especialidad.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AreaEspecialidadService {

    private final AreaEspecialidadRepository areaRepository;

    /**
     * Obtiene todas las áreas de especialidad
     */
    public List<AreaEspecialidad> obtenerTodas() {
        return areaRepository.findAll();
    }

    /**
     * Obtiene un área de especialidad por su ID
     */
    public AreaEspecialidad obtenerPorId(Integer id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", id));
    }

    /**
     * Obtiene áreas de especialidad por nombre (búsqueda parcial)
     */
    public List<AreaEspecialidad> obtenerPorNombreContiene(String nombre) {
        return areaRepository.findAll().stream()
                .filter(a -> a.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();
    }

    /**
     * Crea un nueva área de especialidad
     */
    public AreaEspecialidad crear(AreaEspecialidad area) {
        return areaRepository.save(area);
    }

    /**
     * Actualiza un área de especialidad existente
     */
    public AreaEspecialidad actualizar(Integer id, AreaEspecialidad areaActualizada) {
        AreaEspecialidad area = areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", id));
        
        area.setNombre(areaActualizada.getNombre());
        area.setDescripcion(areaActualizada.getDescripcion());

        return areaRepository.save(area);
    }

    /**
     * Elimina un área de especialidad por su ID
     */
    public void eliminar(Integer id) {
        AreaEspecialidad area = areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", id));
        areaRepository.delete(area);
    }
}
