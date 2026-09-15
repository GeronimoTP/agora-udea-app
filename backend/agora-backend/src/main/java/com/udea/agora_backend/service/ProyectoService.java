package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.ProyectoRequestDTO;
import com.udea.agora_backend.dto.request.VinculacionProyectoRequestDTO;
import com.udea.agora_backend.dto.response.ProyectoResponseDTO;
import com.udea.agora_backend.dto.response.VinculacionProyectoResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de proyectos de investigación.
 * Maneja operaciones CRUD, búsquedas y vinculación de estudiantes a proyectos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final SemilleroRepository semilleroRepository;
    private final EstadoRepository estadoRepository;
    private final EstudianteRepository estudianteRepository;
    private final RolEstudianteProyectoRepository rolEstudianteProyectoRepository;
    private final EstudianteProyectoRepository estudianteProyectoRepository;

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

        // Obtener estado inicial (por defecto Activo o En desarrollo si existe)
        Estado estado = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Activo") || e.getNombre().equalsIgnoreCase("En ejecución"))
                .findFirst()
                .orElseGet(() -> estadoRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Activo")));

        // Crear proyecto
        Proyecto proyecto = Proyecto.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .semillero(semillero)
                .presupuestoAsignado(request.getPresupuestoAsignado())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .estado(estado)
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
        proyecto.setPresupuestoAsignado(request.getPresupuestoAsignado());
        proyecto.setFechaInicio(request.getFechaInicio());
        proyecto.setFechaFin(request.getFechaFin());

        Proyecto proyectoActualizado = proyectoRepository.save(proyecto);
        return mapeoAResponseDTO(proyectoActualizado);
    }

    /**
     * Actualiza el estado de un proyecto
     */
    public ProyectoResponseDTO actualizarEstado(Integer idProyecto, Integer idEstado) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", idProyecto));

        Estado nuevoEstado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", idEstado));

        proyecto.setEstado(nuevoEstado);
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
     * Vincula un estudiante a un proyecto con un rol específico
     */
    public VinculacionProyectoResponseDTO vincularEstudiante(VinculacionProyectoRequestDTO request) {
        Proyecto proyecto = proyectoRepository.findById(request.getIdProyecto())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", request.getIdProyecto()));

        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", request.getIdEstudiante()));

        RolEstudianteProyecto rol = rolEstudianteProyectoRepository.findById(request.getIdRolProyecto())
                .orElseThrow(() -> new RecursoNoEncontradoException("RolEstudianteProyecto", request.getIdRolProyecto()));

        if (estudianteProyectoRepository.findByProyectoIdAndEstudianteId(request.getIdProyecto(), request.getIdEstudiante()).isPresent()) {
            throw new ConflictoException("EstudianteProyecto", "estudianteId", request.getIdEstudiante().toString());
        }

        EstudianteProyecto vinculacion = EstudianteProyecto.builder()
                .proyecto(proyecto)
                .estudiante(estudiante)
                .rolProyecto(rol)
                .fechaVinculacion(LocalDate.now())
                .build();

        EstudianteProyecto guardado = estudianteProyectoRepository.save(vinculacion);

        return VinculacionProyectoResponseDTO.builder()
                .id(guardado.getId())
                .tituloProyecto(proyecto.getTitulo())
                .nombreEstudiante(estudiante.getUsuario().getNombreCompleto())
                .rolProyecto(rol.getNombre())
                .fechaVinculacion(guardado.getFechaVinculacion())
                .build();
    }

    /**
     * Obtiene estudiantes vinculados a un proyecto
     */
    public List<VinculacionProyectoResponseDTO> obtenerEstudiantesVinculados(Integer idProyecto) {
        return estudianteProyectoRepository.findByProyectoId(idProyecto)
                .stream()
                .map(ep -> VinculacionProyectoResponseDTO.builder()
                        .id(ep.getId())
                        .tituloProyecto(ep.getProyecto().getTitulo())
                        .nombreEstudiante(ep.getEstudiante().getUsuario().getNombreCompleto())
                        .rolProyecto(ep.getRolProyecto().getNombre())
                        .fechaVinculacion(ep.getFechaVinculacion())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Desvincula un estudiante de un proyecto
     */
    public void desvincularEstudiante(Integer idProyecto, Integer idEstudiante) {
        EstudianteProyecto vinculacion = estudianteProyectoRepository.findByProyectoIdAndEstudianteId(idProyecto, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteProyecto", "idEstudiante", idEstudiante));
        estudianteProyectoRepository.delete(vinculacion);
    }

    /**
     * Mapea entidad Proyecto a ResponseDTO
     */
    private ProyectoResponseDTO mapeoAResponseDTO(Proyecto proyecto) {
        return ProyectoResponseDTO.builder()
                .id(proyecto.getId())
                .titulo(proyecto.getTitulo())
                .descripcion(proyecto.getDescripcion())
                .nombreSemillero(proyecto.getSemillero() != null ? proyecto.getSemillero().getNombre() : null)
                .presupuestoAsignado(proyecto.getPresupuestoAsignado())
                .fechaInicio(proyecto.getFechaInicio())
                .fechaFin(proyecto.getFechaFin())
                .estadoActual(proyecto.getEstado() != null ? proyecto.getEstado().getNombre() : null)
                .build();
    }
}
