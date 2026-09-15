package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.EstudianteSemilleroRequestDTO;
import com.udea.agora_backend.dto.request.ProfesorSemilleroRequestDTO;
import com.udea.agora_backend.dto.request.SemilleroLineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.SemilleroRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
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
 * Servicio de gestión de semilleros de investigación.
 * Maneja operaciones CRUD, gobernanza/co-tutoría (N:M con profesores),
 * vinculación de estudiantes y asignación de líneas de investigación.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SemilleroService {

    private final SemilleroRepository semilleroRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final EstadoRepository estadoRepository;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;

    /**
     * Obtiene todos los semilleros
     */
    public List<SemilleroResponseDTO> obtenerTodos() {
        return semilleroRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un semillero por su ID
     */
    public SemilleroResponseDTO obtenerPorId(Integer id) {
        Semillero semillero = semilleroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));
        return mapeoAResponseDTO(semillero);
    }

    /**
     * Obtiene un semillero por código identificador
     */
    public SemilleroResponseDTO obtenerPorCodigo(String codigo) {
        Semillero semillero = semilleroRepository.findByCodigoIdentificador(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", "codigo", codigo));
        return mapeoAResponseDTO(semillero);
    }

    /**
     * Obtiene semilleros por programa
     */
    public List<SemilleroResponseDTO> obtenerPorPrograma(Integer idPrograma) {
        return semilleroRepository.findByProgramaId(idPrograma)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene semilleros por estado
     */
    public List<SemilleroResponseDTO> obtenerPorEstado(Integer idEstado) {
        return semilleroRepository.findByEstadoId(idEstado)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo semillero
     */
    public SemilleroResponseDTO crear(SemilleroRequestDTO request) {
        // Verificar código duplicado
        if (semilleroRepository.findByCodigoIdentificador(request.getCodigoIdentificador()).isPresent()) {
            throw new ConflictoException("Semillero", "codigoIdentificador", request.getCodigoIdentificador());
        }

        // Obtener estudiante líder
        Estudiante estudianteLider = estudianteRepository.findById(request.getIdEstudianteLider())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante Líder", request.getIdEstudianteLider()));

        // Obtener programa
        ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));

        // Obtener estado inicial (Activo)
        Estado estado = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Activo"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Activo"));

        // Crear semillero
        Semillero semillero = Semillero.builder()
                .codigoIdentificador(request.getCodigoIdentificador())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .estudianteLider(estudianteLider)
                .programa(programa)
                .estado(estado)
                .createdAt(ZonedDateTime.now())
                .build();

        Semillero semilleroGuardado = semilleroRepository.save(semillero);

        // Guardar líneas de investigación iniciales
        if (request.getIdLineasInvestigacion() != null && !request.getIdLineasInvestigacion().isEmpty()) {
            for (Integer idLinea : request.getIdLineasInvestigacion()) {
                LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                        .orElseThrow(() -> new RecursoNoEncontradoException("LineaInvestigacion", idLinea));

                SemilleroLineaInvestigacion sli = SemilleroLineaInvestigacion.builder()
                        .semillero(semilleroGuardado)
                        .lineaInvestigacion(linea)
                        .build();
                semilleroLineaRepository.save(sli);
            }
        }

        return mapeoAResponseDTO(semilleroGuardado);
    }

    /**
     * Actualiza un semillero existente
     */
    public SemilleroResponseDTO actualizar(Integer id, SemilleroRequestDTO request) {
        Semillero semillero = semilleroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));

        // Verificar código duplicado si cambió
        if (!semillero.getCodigoIdentificador().equals(request.getCodigoIdentificador()) &&
                semilleroRepository.findByCodigoIdentificador(request.getCodigoIdentificador()).isPresent()) {
            throw new ConflictoException("Semillero", "codigoIdentificador", request.getCodigoIdentificador());
        }

        // Obtener estudiante líder si cambió
        if (!semillero.getEstudianteLider().getId().equals(request.getIdEstudianteLider())) {
            Estudiante nuevoLider = estudianteRepository.findById(request.getIdEstudianteLider())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante Líder", request.getIdEstudianteLider()));
            semillero.setEstudianteLider(nuevoLider);
        }

        // Obtener programa si cambió
        if (!semillero.getPrograma().getId().equals(request.getIdPrograma())) {
            ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));
            semillero.setPrograma(programa);
        }

        // Actualizar campos
        semillero.setCodigoIdentificador(request.getCodigoIdentificador());
        semillero.setNombre(request.getNombre());
        semillero.setDescripcion(request.getDescripcion());

        Semillero semilleroActualizado = semilleroRepository.save(semillero);

        // Sincronizar líneas de investigación si vienen en el request
        if (request.getIdLineasInvestigacion() != null) {
            // Eliminar anteriores
            List<SemilleroLineaInvestigacion> anteriores = semilleroLineaRepository.findBySemilleroId(id);
            semilleroLineaRepository.deleteAll(anteriores);

            // Guardar nuevas
            for (Integer idLinea : request.getIdLineasInvestigacion()) {
                LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                        .orElseThrow(() -> new RecursoNoEncontradoException("LineaInvestigacion", idLinea));

                SemilleroLineaInvestigacion sli = SemilleroLineaInvestigacion.builder()
                        .semillero(semilleroActualizado)
                        .lineaInvestigacion(linea)
                        .build();
                semilleroLineaRepository.save(sli);
            }
        }

        return mapeoAResponseDTO(semilleroActualizado);
    }

    /**
     * Actualiza el estado de un semillero
     */
    public SemilleroResponseDTO actualizarEstado(Integer idSemillero, Integer idEstado) {
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));

        Estado estado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", idEstado));

        semillero.setEstado(estado);
        Semillero semilleroActualizado = semilleroRepository.save(semillero);
        return mapeoAResponseDTO(semilleroActualizado);
    }

    /**
     * Elimina un semillero por su ID
     */
    public void eliminar(Integer id) {
        Semillero semillero = semilleroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));
        semilleroRepository.delete(semillero);
    }

    // ==========================================
    // GESTIÓN DE LÍNEAS DE INVESTIGACIÓN
    // ==========================================

    public SemilleroLineaInvestigacionResponseDTO agregarLineaInvestigacion(SemilleroLineaInvestigacionRequestDTO request) {
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        LineaInvestigacion linea = lineaInvestigacionRepository.findById(request.getIdLineaInvestigacion())
                .orElseThrow(() -> new RecursoNoEncontradoException("LineaInvestigacion", request.getIdLineaInvestigacion()));

        if (semilleroLineaRepository.findBySemilleroIdAndLineaInvestigacionId(request.getIdSemillero(), request.getIdLineaInvestigacion()).isPresent()) {
            throw new ConflictoException("SemilleroLineaInvestigacion", "idLineaInvestigacion", request.getIdLineaInvestigacion().toString());
        }

        SemilleroLineaInvestigacion asociacion = SemilleroLineaInvestigacion.builder()
                .semillero(semillero)
                .lineaInvestigacion(linea)
                .build();

        SemilleroLineaInvestigacion guardada = semilleroLineaRepository.save(asociacion);

        return SemilleroLineaInvestigacionResponseDTO.builder()
                .id(guardada.getId())
                .nombreSemillero(semillero.getNombre())
                .nombreLineaInvestigacion(linea.getNombre())
                .build();
    }

    public List<SemilleroLineaInvestigacionResponseDTO> obtenerLineasInvestigacion(Integer idSemillero) {
        return semilleroLineaRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(sli -> SemilleroLineaInvestigacionResponseDTO.builder()
                        .id(sli.getId())
                        .nombreSemillero(sli.getSemillero().getNombre())
                        .nombreLineaInvestigacion(sli.getLineaInvestigacion().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public void eliminarLineaInvestigacion(Integer idSemillero, Integer idLinea) {
        SemilleroLineaInvestigacion asociacion = semilleroLineaRepository.findBySemilleroIdAndLineaInvestigacionId(idSemillero, idLinea)
                .orElseThrow(() -> new RecursoNoEncontradoException("SemilleroLineaInvestigacion", "idLinea", idLinea));
        semilleroLineaRepository.delete(asociacion);
    }

    // ==========================================
    // CO-TUTORÍA Y GOBERNANZA (PROFESORES)
    // ==========================================

    public ProfesorSemilleroResponseDTO asignarProfesor(ProfesorSemilleroRequestDTO request) {
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        Profesor profesor = profesorRepository.findById(request.getIdProfesor())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", request.getIdProfesor()));

        if (profesorSemilleroRepository.findBySemilleroIdAndProfesorId(request.getIdSemillero(), request.getIdProfesor()).isPresent()) {
            throw new ConflictoException("ProfesorSemillero", "idProfesor", request.getIdProfesor().toString());
        }

        ProfesorSemillero asociacion = ProfesorSemillero.builder()
                .semillero(semillero)
                .profesor(profesor)
                .fechaAsignacion(request.getFechaAsignacion() != null ? request.getFechaAsignacion() : LocalDate.now())
                .build();

        ProfesorSemillero guardado = profesorSemilleroRepository.save(asociacion);

        return ProfesorSemilleroResponseDTO.builder()
                .id(guardado.getId())
                .nombreSemillero(semillero.getNombre())
                .nombreProfesor(profesor.getUsuario().getNombreCompleto())
                .fechaAsignacion(guardado.getFechaAsignacion())
                .build();
    }

    public List<ProfesorSemilleroResponseDTO> obtenerProfesores(Integer idSemillero) {
        return profesorSemilleroRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(ps -> ProfesorSemilleroResponseDTO.builder()
                        .id(ps.getId())
                        .nombreSemillero(ps.getSemillero().getNombre())
                        .nombreProfesor(ps.getProfesor().getUsuario().getNombreCompleto())
                        .fechaAsignacion(ps.getFechaAsignacion())
                        .build())
                .collect(Collectors.toList());
    }

    public void removerProfesor(Integer idSemillero, Integer idProfesor) {
        ProfesorSemillero asociacion = profesorSemilleroRepository.findBySemilleroIdAndProfesorId(idSemillero, idProfesor)
                .orElseThrow(() -> new RecursoNoEncontradoException("ProfesorSemillero", "idProfesor", idProfesor));
        profesorSemilleroRepository.delete(asociacion);
    }

    // ==========================================
    // VINCULACIÓN DE ESTUDIANTES AL SEMILLERO
    // ==========================================

    public EstudianteSemilleroResponseDTO vincularEstudiante(EstudianteSemilleroRequestDTO request) {
        Semillero semillero = semilleroRepository.findById(request.getIdSemillero())
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", request.getIdSemillero()));

        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", request.getIdEstudiante()));

        Estado estado = estadoRepository.findById(request.getIdEstado())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", request.getIdEstado()));

        if (estudianteSemilleroRepository.findBySemilleroIdAndEstudianteId(request.getIdSemillero(), request.getIdEstudiante()).isPresent()) {
            throw new ConflictoException("EstudianteSemillero", "idEstudiante", request.getIdEstudiante().toString());
        }

        EstudianteSemillero vinculacion = EstudianteSemillero.builder()
                .semillero(semillero)
                .estudiante(estudiante)
                .fechaIngreso(request.getFechaIngreso() != null ? request.getFechaIngreso() : LocalDate.now())
                .fechaSalida(request.getFechaSalida())
                .estado(estado)
                .build();

        EstudianteSemillero guardado = estudianteSemilleroRepository.save(vinculacion);

        return EstudianteSemilleroResponseDTO.builder()
                .id(guardado.getId())
                .nombreSemillero(semillero.getNombre())
                .nombreEstudiante(estudiante.getUsuario().getNombreCompleto())
                .fechaIngreso(guardado.getFechaIngreso())
                .fechaSalida(guardado.getFechaSalida())
                .estadoVinculacion(estado.getNombre())
                .build();
    }

    public List<EstudianteSemilleroResponseDTO> obtenerEstudiantes(Integer idSemillero) {
        return estudianteSemilleroRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(es -> EstudianteSemilleroResponseDTO.builder()
                        .id(es.getId())
                        .nombreSemillero(es.getSemillero().getNombre())
                        .nombreEstudiante(es.getEstudiante().getUsuario().getNombreCompleto())
                        .fechaIngreso(es.getFechaIngreso())
                        .fechaSalida(es.getFechaSalida())
                        .estadoVinculacion(es.getEstado() != null ? es.getEstado().getNombre() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public EstudianteSemilleroResponseDTO actualizarVinculacionEstudiante(Integer idSemillero, Integer idEstudiante, Integer idEstado, LocalDate fechaSalida) {
        EstudianteSemillero vinculacion = estudianteSemilleroRepository.findBySemilleroIdAndEstudianteId(idSemillero, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteSemillero", "idEstudiante", idEstudiante));

        Estado estado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", idEstado));

        vinculacion.setEstado(estado);
        if (fechaSalida != null) {
            vinculacion.setFechaSalida(fechaSalida);
        }

        EstudianteSemillero actualizado = estudianteSemilleroRepository.save(vinculacion);

        return EstudianteSemilleroResponseDTO.builder()
                .id(actualizado.getId())
                .nombreSemillero(actualizado.getSemillero().getNombre())
                .nombreEstudiante(actualizado.getEstudiante().getUsuario().getNombreCompleto())
                .fechaIngreso(actualizado.getFechaIngreso())
                .fechaSalida(actualizado.getFechaSalida())
                .estadoVinculacion(actualizado.getEstado().getNombre())
                .build();
    }

    /**
     * Mapea entidad Semillero a ResponseDTO
     */
    private SemilleroResponseDTO mapeoAResponseDTO(Semillero semillero) {
        // Obtener líneas de investigación
        List<String> lineasInvestigacion = semilleroLineaRepository.findBySemilleroId(semillero.getId())
                .stream()
                .map(sli -> sli.getLineaInvestigacion().getNombre())
                .collect(Collectors.toList());

        return SemilleroResponseDTO.builder()
                .id(semillero.getId())
                .codigoIdentificador(semillero.getCodigoIdentificador())
                .nombre(semillero.getNombre())
                .descripcion(semillero.getDescripcion())
                .nombreEstudianteLider(semillero.getEstudianteLider() != null && semillero.getEstudianteLider().getUsuario() != null ? semillero.getEstudianteLider().getUsuario().getNombreCompleto() : null)
                .nombrePrograma(semillero.getPrograma() != null ? semillero.getPrograma().getNombre() : null)
                .estadoActual(semillero.getEstado() != null ? semillero.getEstado().getNombre() : null)
                .lineasInvestigacion(lineasInvestigacion)
                .build();
    }
}
