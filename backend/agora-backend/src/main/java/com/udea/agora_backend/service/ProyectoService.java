package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.CategoriasEstado;
import com.udea.agora_backend.dominio.EstadosProyecto;
import com.udea.agora_backend.dto.request.ProyectoRequestDTO;
import com.udea.agora_backend.dto.request.VinculacionProyectoRequestDTO;
import com.udea.agora_backend.dto.response.ProyectoResponseDTO;
import com.udea.agora_backend.dto.response.VinculacionProyectoResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteProyecto;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.RolEstudianteProyecto;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.EstudianteProyectoRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.PublicacionRepository;
import com.udea.agora_backend.repository.RolEstudianteProyectoRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.mapper.ProyectoMapper;
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
    private final BuscadorEstados buscadorEstados;
    private final EstudianteRepository estudianteRepository;
    private final RolEstudianteProyectoRepository rolEstudianteProyectoRepository;
    private final EstudianteProyectoRepository estudianteProyectoRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final PublicacionRepository publicacionRepository;
    private final ProyectoMapper proyectoMapper;

    /**
     * Obtiene todos los proyectos
     */
    public List<ProyectoResponseDTO> obtenerTodos() {
        return proyectoMapper.aResponses(proyectoRepository.findAll());
    }

    /**
     * Obtiene un proyecto por su ID
     */
    public ProyectoResponseDTO obtenerPorId(Integer id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
        return proyectoMapper.aResponse(proyecto);
    }

    /**
     * Obtiene proyectos de un semillero
     */
    public List<ProyectoResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return proyectoMapper.aResponses(proyectoRepository.findBySemilleroId(idSemillero));
    }

    /**
     * Crea un nuevo proyecto
     */
    public ProyectoResponseDTO crear(ProyectoRequestDTO request) {
        // Obtener semillero
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));
        validarFechas(request);

        // Estado inicial del proyecto
        Estado estado = buscadorEstados.porNombre(CategoriasEstado.PROYECTO, EstadosProyecto.EN_EJECUCION);

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
        return proyectoMapper.aResponse(proyectoGuardado);
    }

    /**
     * Actualiza un proyecto existente
     */
    public ProyectoResponseDTO actualizar(Integer id, ProyectoRequestDTO request) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));

        // Sus integrantes y publicaciones pertenecen al semillero: un proyecto no cambia de semillero
        if (!proyecto.getSemillero().getId().equals(request.getIdSemillero())) {
            throw new ErrorLogicaNegocioException("Un proyecto no puede cambiar de semillero.");
        }
        validarFechas(request);

        // Actualizar campos
        proyecto.setTitulo(request.getTitulo());
        proyecto.setDescripcion(request.getDescripcion());
        proyecto.setPresupuestoAsignado(request.getPresupuestoAsignado());
        proyecto.setFechaInicio(request.getFechaInicio());
        proyecto.setFechaFin(request.getFechaFin());

        Proyecto proyectoActualizado = proyectoRepository.save(proyecto);
        return proyectoMapper.aResponse(proyectoActualizado);
    }

    /**
     * Actualiza el estado de un proyecto
     */
    public ProyectoResponseDTO actualizarEstado(Integer idProyecto, Integer idEstado) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", idProyecto));

        Estado nuevoEstado = buscadorEstados.porIdEnCategoria(CategoriasEstado.PROYECTO, idEstado);

        proyecto.setEstado(nuevoEstado);
        Proyecto proyectoActualizado = proyectoRepository.save(proyecto);
        return proyectoMapper.aResponse(proyectoActualizado);
    }

    /**
     * Elimina un proyecto por su ID
     */
    public void eliminar(Integer id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", id));
        if (!publicacionRepository.findByProyectoId(id).isEmpty()) {
            throw new ErrorLogicaNegocioException(
                    "No se puede eliminar un proyecto con publicaciones; cambia su estado a 'Cancelado' o 'Completado'.");
        }
        estudianteProyectoRepository.deleteByProyectoId(id);
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

        // Solo integrantes activos del semillero (o su estudiante líder) trabajan en sus proyectos
        Semillero semillero = proyecto.getSemillero();
        boolean esLider = semillero.getEstudianteLider().getId().equals(estudiante.getId());
        if (!esLider && !estudianteSemilleroRepository.existsBySemilleroIdAndEstudianteIdAndFechaSalidaIsNull(
                semillero.getId(), estudiante.getId())) {
            throw new ErrorLogicaNegocioException(
                    "Solo los integrantes activos del semillero pueden vincularse a sus proyectos.");
        }

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

        return proyectoMapper.aVinculacion(guardado);
    }

    /**
     * Obtiene estudiantes vinculados a un proyecto
     */
    public List<VinculacionProyectoResponseDTO> obtenerEstudiantesVinculados(Integer idProyecto) {
        return estudianteProyectoRepository.findByProyectoId(idProyecto)
                .stream()
                .map(proyectoMapper::aVinculacion)
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

    private void validarFechas(ProyectoRequestDTO request) {
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new ErrorLogicaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
    }
}
