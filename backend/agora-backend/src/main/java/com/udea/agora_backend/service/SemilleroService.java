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
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    private static final String ESTADO_SEMILLERO_ACTIVO = "Activo";
    private static final String ESTADO_CONVOCATORIA_ABIERTA = "Abierta";
    private static final String ESTADO_VINCULACION_ACTIVA = "Activo";
    private static final String ESTADO_VINCULACION_INACTIVA = "Inactivo";

    private final SemilleroRepository semilleroRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final EstadoRepository estadoRepository;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final ProyectoRepository proyectoRepository;
    private final PublicacionRepository publicacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final ReconocimientoSemilleroRepository reconocimientoRepository;

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
     * Crea un nuevo semillero con al menos un profesor tutor ya registrado.
     * Si lo crea un profesor (idProfesorCreador no nulo), queda vinculado como tutor
     * aunque no venga en la lista. Un estudiante solo puede liderar un semillero.
     */
    public SemilleroResponseDTO crear(SemilleroRequestDTO request, Integer idProfesorCreador) {
        // Verificar código duplicado
        if (semilleroRepository.findByCodigoIdentificador(request.getCodigoIdentificador()).isPresent()) {
            throw new ConflictoException("Semillero", "codigoIdentificador", request.getCodigoIdentificador());
        }

        // Obtener estudiante líder
        Estudiante estudianteLider = estudianteRepository.findById(request.getIdEstudianteLider())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante Líder", request.getIdEstudianteLider()));
        validarSinOtroLiderazgo(estudianteLider.getId());

        // Profesores tutores (obligatorio al menos uno)
        Set<Integer> idsProfesores = new LinkedHashSet<>();
        if (request.getIdProfesores() != null) {
            request.getIdProfesores().stream().filter(Objects::nonNull).forEach(idsProfesores::add);
        }
        if (idProfesorCreador != null) {
            idsProfesores.add(idProfesorCreador);
        }
        if (idsProfesores.isEmpty()) {
            throw new ErrorLogicaNegocioException("El semillero debe tener al menos un profesor tutor.");
        }
        List<Profesor> profesores = idsProfesores.stream()
                .map(idProfesor -> profesorRepository.findById(idProfesor)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Profesor", idProfesor)))
                .toList();

        // Obtener programa
        ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", request.getIdPrograma()));

        // Obtener estado inicial (Activo)
        Estado estado = buscarEstado(CategoriasEstado.SEMILLERO, ESTADO_SEMILLERO_ACTIVO);

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

        for (Profesor profesor : profesores) {
            profesorSemilleroRepository.save(ProfesorSemillero.builder()
                    .semillero(semilleroGuardado)
                    .profesor(profesor)
                    .fechaAsignacion(LocalDate.now())
                    .build());
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

        // El líder solo cambia con transferirLiderazgo, que exige un integrante activo
        if (!semillero.getEstudianteLider().getId().equals(request.getIdEstudianteLider())) {
            throw new ErrorLogicaNegocioException(
                    "Para cambiar el líder usa la transferencia de liderazgo.");
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
        if (!CategoriasEstado.SEMILLERO.equalsIgnoreCase(estado.getCategoriaEstado().getNombre())) {
            throw new ErrorLogicaNegocioException("El estado '" + estado.getNombre() + "' no es un estado de semillero.");
        }
        if (!ESTADO_SEMILLERO_ACTIVO.equalsIgnoreCase(estado.getNombre()) && tieneConvocatoriaAbierta(idSemillero)) {
            throw new ErrorLogicaNegocioException(
                    "Cierra o cancela las convocatorias abiertas antes de cambiar el semillero a '" + estado.getNombre() + "'.");
        }

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
        // Con historial (convocatorias, proyectos, integrantes o reconocimientos) se inactiva, no se borra
        if (!convocatoriaRepository.findBySemilleroId(id).isEmpty()
                || !proyectoRepository.findBySemilleroId(id).isEmpty()
                || !estudianteSemilleroRepository.findBySemilleroId(id).isEmpty()
                || !reconocimientoRepository.findBySemilleroId(id).isEmpty()) {
            throw new ErrorLogicaNegocioException(
                    "El semillero tiene historial; cámbialo a 'Inactivo' en lugar de eliminarlo.");
        }
        semilleroLineaRepository.deleteAll(semilleroLineaRepository.findBySemilleroId(id));
        profesorSemilleroRepository.deleteAll(profesorSemilleroRepository.findBySemilleroId(id));
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
                .idLineaInvestigacion(linea.getId())
                .nombreLineaInvestigacion(linea.getNombre())
                .build();
    }

    public List<SemilleroLineaInvestigacionResponseDTO> obtenerLineasInvestigacion(Integer idSemillero) {
        return semilleroLineaRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(sli -> SemilleroLineaInvestigacionResponseDTO.builder()
                        .id(sli.getId())
                        .nombreSemillero(sli.getSemillero().getNombre())
                        .idLineaInvestigacion(sli.getLineaInvestigacion().getId())
                        .nombreLineaInvestigacion(sli.getLineaInvestigacion().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public void eliminarLineaInvestigacion(Integer idSemillero, Integer idLinea) {
        SemilleroLineaInvestigacion asociacion = semilleroLineaRepository.findBySemilleroIdAndLineaInvestigacionId(idSemillero, idLinea)
                .orElseThrow(() -> new RecursoNoEncontradoException("SemilleroLineaInvestigacion", "idLinea", idLinea));
        if (semilleroLineaRepository.findBySemilleroId(idSemillero).size() <= 1) {
            throw new ErrorLogicaNegocioException("El semillero debe conservar al menos una línea de investigación.");
        }
        semilleroLineaRepository.delete(asociacion);
    }

    private boolean tieneConvocatoriaAbierta(Integer idSemillero) {
        return convocatoriaRepository.findBySemilleroId(idSemillero).stream()
                .anyMatch(c -> ESTADO_CONVOCATORIA_ABIERTA.equalsIgnoreCase(c.getEstado().getNombre()));
    }

    // Un estudiante lidera como máximo un semillero
    private void validarSinOtroLiderazgo(Integer idEstudiante) {
        if (!semilleroRepository.findIdsByEstudianteLiderId(idEstudiante).isEmpty()) {
            throw new ErrorLogicaNegocioException("El estudiante ya es líder de otro semillero.");
        }
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
                .idProfesor(profesor.getId())
                .idUsuario(profesor.getUsuario().getId())
                .nombreProfesor(profesor.getUsuario().getNombreCompleto())
                .email(profesor.getUsuario().getEmail())
                .fechaAsignacion(guardado.getFechaAsignacion())
                .build();
    }

    public List<ProfesorSemilleroResponseDTO> obtenerProfesores(Integer idSemillero) {
        return profesorSemilleroRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(ps -> ProfesorSemilleroResponseDTO.builder()
                        .id(ps.getId())
                        .nombreSemillero(ps.getSemillero().getNombre())
                        .idProfesor(ps.getProfesor().getId())
                        .idUsuario(ps.getProfesor().getUsuario().getId())
                        .nombreProfesor(ps.getProfesor().getUsuario().getNombreCompleto())
                        .email(ps.getProfesor().getUsuario().getEmail())
                        .fechaAsignacion(ps.getFechaAsignacion())
                        .build())
                .collect(Collectors.toList());
    }

    public void removerProfesor(Integer idSemillero, Integer idProfesor) {
        ProfesorSemillero asociacion = profesorSemilleroRepository.findBySemilleroIdAndProfesorId(idSemillero, idProfesor)
                .orElseThrow(() -> new RecursoNoEncontradoException("ProfesorSemillero", "idProfesor", idProfesor));
        if (profesorSemilleroRepository.findBySemilleroId(idSemillero).size() <= 1) {
            throw new ErrorLogicaNegocioException("El semillero debe conservar al menos un profesor tutor.");
        }
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

        Estado estado = buscarEstadoVinculacion(request.getIdEstado());

        if (semillero.getEstudianteLider().getId().equals(estudiante.getId())) {
            throw new ErrorLogicaNegocioException("El estudiante líder no se vincula como integrante.");
        }
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

        return mapearVinculacion(estudianteSemilleroRepository.save(vinculacion));
    }

    public List<EstudianteSemilleroResponseDTO> obtenerEstudiantes(Integer idSemillero) {
        return estudianteSemilleroRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(this::mapearVinculacion)
                .collect(Collectors.toList());
    }

    public EstudianteSemilleroResponseDTO actualizarVinculacionEstudiante(Integer idSemillero, Integer idEstudiante, Integer idEstado, LocalDate fechaSalida) {
        EstudianteSemillero vinculacion = estudianteSemilleroRepository.findBySemilleroIdAndEstudianteId(idSemillero, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteSemillero", "idEstudiante", idEstudiante));

        vinculacion.setEstado(buscarEstadoVinculacion(idEstado));
        if (fechaSalida != null) {
            vinculacion.setFechaSalida(fechaSalida);
        }

        return mapearVinculacion(estudianteSemilleroRepository.save(vinculacion));
    }

    /**
     * Retira a un integrante: la vinculación se conserva como historial con fecha de salida
     * y estado "Inactivo". Un integrante retirado puede volver a postularse.
     */
    public EstudianteSemilleroResponseDTO retirarEstudiante(Integer idSemillero, Integer idEstudiante) {
        EstudianteSemillero vinculacion = estudianteSemilleroRepository.findBySemilleroIdAndEstudianteId(idSemillero, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteSemillero", "idEstudiante", idEstudiante));
        if (vinculacion.getFechaSalida() != null) {
            throw new ErrorLogicaNegocioException("El estudiante ya fue retirado del semillero.");
        }
        vinculacion.setFechaSalida(LocalDate.now());
        vinculacion.setEstado(buscarEstado(CategoriasEstado.VINCULACION, ESTADO_VINCULACION_INACTIVA));
        return mapearVinculacion(estudianteSemilleroRepository.save(vinculacion));
    }

    /**
     * Transfiere el liderazgo estudiantil a un integrante activo. El líder saliente queda
     * como integrante activo del semillero, para no perder su historial.
     */
    public SemilleroResponseDTO transferirLiderazgo(Integer idSemillero, Integer idNuevoLider) {
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        Estudiante liderActual = semillero.getEstudianteLider();
        if (liderActual.getId().equals(idNuevoLider)) {
            throw new ErrorLogicaNegocioException("El estudiante ya es el líder del semillero.");
        }
        if (!estudianteSemilleroRepository.existsBySemilleroIdAndEstudianteIdAndFechaSalidaIsNull(idSemillero, idNuevoLider)) {
            throw new ErrorLogicaNegocioException("El nuevo líder debe ser un integrante activo del semillero.");
        }
        validarSinOtroLiderazgo(idNuevoLider);
        Estudiante nuevoLider = estudianteRepository.findById(idNuevoLider)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", idNuevoLider));

        Estado activo = buscarEstado(CategoriasEstado.VINCULACION, ESTADO_VINCULACION_ACTIVA);
        EstudianteSemillero vinculacionSaliente = estudianteSemilleroRepository
                .findBySemilleroIdAndEstudianteId(idSemillero, liderActual.getId())
                .orElseGet(() -> EstudianteSemillero.builder()
                        .semillero(semillero)
                        .estudiante(liderActual)
                        .fechaIngreso(LocalDate.now())
                        .build());
        vinculacionSaliente.setFechaSalida(null);
        vinculacionSaliente.setEstado(activo);
        estudianteSemilleroRepository.save(vinculacionSaliente);

        semillero.setEstudianteLider(nuevoLider);
        return mapeoAResponseDTO(semilleroRepository.save(semillero));
    }

    private EstudianteSemilleroResponseDTO mapearVinculacion(EstudianteSemillero es) {
        Estudiante estudiante = es.getEstudiante();
        return EstudianteSemilleroResponseDTO.builder()
                .id(es.getId())
                .idSemillero(es.getSemillero().getId())
                .nombreSemillero(es.getSemillero().getNombre())
                .idEstudiante(estudiante.getId())
                .idUsuario(estudiante.getUsuario().getId())
                .nombreEstudiante(estudiante.getUsuario().getNombreCompleto())
                .email(estudiante.getUsuario().getEmail())
                .nombrePrograma(estudiante.getPrograma() != null ? estudiante.getPrograma().getNombre() : null)
                .semestre(estudiante.getSemestre())
                .fechaIngreso(es.getFechaIngreso())
                .fechaSalida(es.getFechaSalida())
                .estadoVinculacion(es.getEstado() != null ? es.getEstado().getNombre() : null)
                .build();
    }

    private Estado buscarEstadoVinculacion(Integer idEstado) {
        Estado estado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", idEstado));
        if (!CategoriasEstado.VINCULACION.equalsIgnoreCase(estado.getCategoriaEstado().getNombre())) {
            throw new ErrorLogicaNegocioException("El estado '" + estado.getNombre() + "' no es válido para una vinculación.");
        }
        return estado;
    }

    private Estado buscarEstado(String categoria, String nombre) {
        return estadoRepository.findByCategoriaEstadoNombreIgnoreCaseAndNombreIgnoreCase(categoria, nombre)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", categoria + " / " + nombre));
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
                .idEstudianteLider(semillero.getEstudianteLider() != null ? semillero.getEstudianteLider().getId() : null)
                .idUsuarioLider(semillero.getEstudianteLider() != null && semillero.getEstudianteLider().getUsuario() != null ? semillero.getEstudianteLider().getUsuario().getId() : null)
                .nombreEstudianteLider(semillero.getEstudianteLider() != null && semillero.getEstudianteLider().getUsuario() != null ? semillero.getEstudianteLider().getUsuario().getNombreCompleto() : null)
                .idPrograma(semillero.getPrograma() != null ? semillero.getPrograma().getId() : null)
                .nombrePrograma(semillero.getPrograma() != null ? semillero.getPrograma().getNombre() : null)
                .estadoActual(semillero.getEstado() != null ? semillero.getEstado().getNombre() : null)
                .lineasInvestigacion(lineasInvestigacion)
                .nombreFacultad(semillero.getPrograma() != null && semillero.getPrograma().getFacultad() != null
                        ? semillero.getPrograma().getFacultad().getNombre() : null)
                .totalProyectos(proyectoRepository.findBySemilleroId(semillero.getId()).size())
                .totalPublicaciones(publicacionRepository.findByProyectoSemilleroId(semillero.getId()).size())
                .totalIntegrantes(estudianteSemilleroRepository.findBySemilleroIdAndFechaSalidaIsNull(semillero.getId()).size())
                .convocatoriaAbierta(convocatoriaRepository.findBySemilleroId(semillero.getId()).stream()
                        .anyMatch(c -> c.getEstado() != null && ESTADO_CONVOCATORIA_ABIERTA.equalsIgnoreCase(c.getEstado().getNombre())
                                && c.getCuposDisponibles() > 0
                                && !c.getFechaCierre().isBefore(LocalDate.now())))
                .build();
    }
}
