package com.udea.agora_backend.service;

import com.udea.agora_backend.model.LineaInvestigacion;
import com.udea.agora_backend.repository.LineaInvestigacionRepository;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de gestión de líneas de investigación.
 * Maneja operaciones CRUD simples de líneas de investigación.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LineaInvestigacionService {

    private final LineaInvestigacionRepository lineaRepository;

    /**
     * Obtiene todas las líneas de investigación
     */
    public List<LineaInvestigacion> obtenerTodas() {
        return lineaRepository.findAll();
    }

    /**
     * Obtiene una línea de investigación por su ID
     */
    public LineaInvestigacion obtenerPorId(Integer id) {
        return lineaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Línea de Investigación", id));
    }

    /**
     * Obtiene líneas de investigación por nombre (búsqueda parcial)
     */
    public List<LineaInvestigacion> obtenerPorNombreContiene(String nombre) {
        return lineaRepository.findAll().stream()
                .filter(l -> l.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .toList();
    }

    /**
     * Crea una nueva línea de investigación
     */
    public LineaInvestigacion crear(LineaInvestigacion linea) {
        return lineaRepository.save(linea);
    }

    /**
     * Actualiza una línea de investigación existente
     */
    public LineaInvestigacion actualizar(Integer id, LineaInvestigacion lineaActualizada) {
        LineaInvestigacion linea = lineaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Línea de Investigación", id));
        
        linea.setNombre(lineaActualizada.getNombre());
        linea.setDescripcion(lineaActualizada.getDescripcion());
        linea.setAreaEspecialidad(lineaActualizada.getAreaEspecialidad());

        return lineaRepository.save(linea);
    }

    /**
     * Elimina una línea de investigación por su ID
     */
    public void eliminar(Integer id) {
        LineaInvestigacion linea = lineaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Línea de Investigación", id));
        lineaRepository.delete(linea);
    }
}
