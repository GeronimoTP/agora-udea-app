package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.TipoElementoCatalogo;
import com.udea.agora_backend.dto.request.LineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.response.LineaInvestigacionResponseDTO;
import com.udea.agora_backend.evento.SemillerosModificadosEvent;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.AreaEspecialidad;
import com.udea.agora_backend.model.LineaInvestigacion;
import com.udea.agora_backend.repository.AreaEspecialidadRepository;
import com.udea.agora_backend.repository.LineaInvestigacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de líneas de investigación. Cada línea pertenece a un área de especialidad; cambiar
 * esa área o eliminar la línea mueve a los semilleros en el árbol KD, que se invalida.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LineaInvestigacionService {

    private final LineaInvestigacionRepository lineaRepository;
    private final AreaEspecialidadRepository areaRepository;
    private final ApplicationEventPublisher eventos;
    private final NombresCatalogo nombresCatalogo;

    @Transactional(readOnly = true)
    public List<LineaInvestigacionResponseDTO> obtenerTodas() {
        return lineaRepository.findAll().stream().map(this::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public LineaInvestigacionResponseDTO obtenerPorId(Integer id) {
        return aResponse(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<LineaInvestigacionResponseDTO> buscarPorNombre(String nombre) {
        return lineaRepository.findByNombreContainingIgnoreCaseOrderByNombre(nombre).stream()
                .map(this::aResponse)
                .toList();
    }

    public LineaInvestigacionResponseDTO crear(LineaInvestigacionRequestDTO request) {
        nombresCatalogo.validarLibre(TipoElementoCatalogo.LINEA_INVESTIGACION, request.getNombre());
        LineaInvestigacion linea = LineaInvestigacion.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .areaEspecialidad(buscarArea(request.getIdAreaEspecialidad()))
                .build();
        return aResponse(lineaRepository.save(linea));
    }

    public LineaInvestigacionResponseDTO actualizar(Integer id, LineaInvestigacionRequestDTO request) {
        LineaInvestigacion linea = buscar(id);
        nombresCatalogo.validarRenombre(TipoElementoCatalogo.LINEA_INVESTIGACION, linea.getNombre(), request.getNombre());
        linea.setNombre(request.getNombre());
        linea.setDescripcion(request.getDescripcion());
        linea.setAreaEspecialidad(buscarArea(request.getIdAreaEspecialidad()));
        publicarCambioSemilleros();
        return aResponse(lineaRepository.save(linea));
    }

    public void eliminar(Integer id) {
        lineaRepository.delete(buscar(id));
        publicarCambioSemilleros();
    }

    private LineaInvestigacion buscar(Integer id) {
        return lineaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Línea de Investigación", id));
    }

    private AreaEspecialidad buscarArea(Integer idArea) {
        return areaRepository.findById(idArea)
                .orElseThrow(() -> new RecursoNoEncontradoException("Área de Especialidad", idArea));
    }

    private LineaInvestigacionResponseDTO aResponse(LineaInvestigacion linea) {
        AreaEspecialidad area = linea.getAreaEspecialidad();
        return LineaInvestigacionResponseDTO.builder()
                .id(linea.getId())
                .nombre(linea.getNombre())
                .descripcion(linea.getDescripcion())
                .idAreaEspecialidad(area != null ? area.getId() : null)
                .nombreAreaEspecialidad(area != null ? area.getNombre() : null)
                .build();
    }

    // Invalida el árbol KD de semilleros similares. El índice escucha el evento después del
    // commit: si la operación falla y se revierte, el árbol no se descarta.
    private void publicarCambioSemilleros() {
        eventos.publishEvent(new SemillerosModificadosEvent());
    }
}
