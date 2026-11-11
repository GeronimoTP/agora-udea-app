package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.CategoriasEstado;
import com.udea.agora_backend.dominio.EstadosSemillero;
import com.udea.agora_backend.dto.request.SemilleroLineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.SemilleroRequestDTO;
import com.udea.agora_backend.dto.response.SemilleroLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.evento.SemillerosModificadosEvent;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.LineaInvestigacion;
import com.udea.agora_backend.model.Profesor;
import com.udea.agora_backend.model.ProfesorSemillero;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.SemilleroLineaInvestigacion;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.LineaInvestigacionRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.ProgramaAcademicoRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.ReconocimientoSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroLineaInvestigacionRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.service.mapper.SemilleroMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Datos del semillero: registro, edición, estado, eliminación y líneas de investigación.
 * Tutores, integrantes y liderazgo están en {@link MiembrosSemilleroService}; la aprobación del
 * administrador, en {@link AprobacionSemillerosService}; la conversión a DTOs, en {@link SemilleroMapper}.
 *
 * <p>Un semillero pendiente de aprobación o rechazado solo lo ven sus líderes y el
 * administrador ({@link VisibilidadSemilleros}); para los demás no existe (404).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SemilleroService {

    private final SemilleroRepository semilleroRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final BuscadorEstados buscadorEstados;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final SemilleroLineaInvestigacionRepository semilleroLineaRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final ProyectoRepository proyectoRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final ReconocimientoSemilleroRepository reconocimientoRepository;
    private final LiderazgoSemilleroService liderazgoSemilleroService;
    private final SemilleroMapper semilleroMapper;
    private final ApplicationEventPublisher eventos;

    /** Semilleros aprobados, más los no aprobados que el usuario puede ver. */
    @Transactional(readOnly = true)
    public List<SemilleroResponseDTO> obtenerTodos(VisibilidadSemilleros visibilidad) {
        return semilleroMapper.aResponses(visibles(semilleroRepository.findAll(), visibilidad));
    }

    @Transactional(readOnly = true)
    public SemilleroResponseDTO obtenerPorId(Integer id, VisibilidadSemilleros visibilidad) {
        Semillero semillero = semilleroRepository.findById(id)
                .filter(visibilidad::puedeVer)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", id));
        return semilleroMapper.aResponse(semillero);
    }

    @Transactional(readOnly = true)
    public SemilleroResponseDTO obtenerPorCodigo(String codigo, VisibilidadSemilleros visibilidad) {
        Semillero semillero = semilleroRepository.findByCodigoIdentificador(codigo)
                .filter(visibilidad::puedeVer)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", "codigo", codigo));
        return semilleroMapper.aResponse(semillero);
    }

    @Transactional(readOnly = true)
    public List<SemilleroResponseDTO> obtenerPorPrograma(Integer idPrograma, VisibilidadSemilleros visibilidad) {
        return semilleroMapper.aResponses(visibles(semilleroRepository.findByProgramaId(idPrograma), visibilidad));
    }

    @Transactional(readOnly = true)
    public List<SemilleroResponseDTO> obtenerPorEstado(Integer idEstado, VisibilidadSemilleros visibilidad) {
        return semilleroMapper.aResponses(visibles(semilleroRepository.findByEstadoId(idEstado), visibilidad));
    }

    private static List<Semillero> visibles(List<Semillero> semilleros, VisibilidadSemilleros visibilidad) {
        return semilleros.stream().filter(visibilidad::puedeVer).toList();
    }

    /**
     * Registra un semillero con al menos un profesor tutor ya registrado. Nace "Pendiente de
     * aprobación": el administrador debe aprobarlo antes de que sea público y opere.
     * Si lo crea un profesor (idProfesorCreador no nulo), queda vinculado como tutor
     * aunque no venga en la lista. Un estudiante solo puede liderar un semillero (cuentan
     * también los pendientes y rechazados).
     */
    public SemilleroResponseDTO crear(SemilleroRequestDTO request, Integer idProfesorCreador) {
        publicarCambioSemilleros();
        // Verificar código duplicado
        if (semilleroRepository.findByCodigoIdentificador(request.getCodigoIdentificador()).isPresent()) {
            throw new ConflictoException("Semillero", "codigoIdentificador", request.getCodigoIdentificador());
        }

        // Obtener estudiante líder
        Estudiante estudianteLider = estudianteRepository.findById(request.getIdEstudianteLider())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante Líder", request.getIdEstudianteLider()));
        liderazgoSemilleroService.validarQueNoLideraOtroSemillero(estudianteLider.getId());

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

        Estado estado = buscadorEstados.deSemillero(EstadosSemillero.PENDIENTE_APROBACION);

        // Crear semillero
        Semillero semillero = Semillero.builder()
                .codigoIdentificador(request.getCodigoIdentificador())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .estudianteLider(estudianteLider)
                .programa(programa)
                .estado(estado)
                .createdAt(ZonedDateTime.now())
                .fechaSolicitud(ZonedDateTime.now())
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

        return semilleroMapper.aResponse(semilleroGuardado);
    }

    /**
     * Actualiza un semillero existente
     */
    public SemilleroResponseDTO actualizar(Integer id, SemilleroRequestDTO request) {
        publicarCambioSemilleros();
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

        return semilleroMapper.aResponse(semilleroActualizado);
    }

    /**
     * Cambia el estado de un semillero aprobado entre Activo e Inactivo. La aprobación y el
     * rechazo son del administrador ({@link AprobacionSemillerosService}).
     */
    public SemilleroResponseDTO actualizarEstado(Integer idSemillero, Integer idEstado) {
        publicarCambioSemilleros();
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        if (!semillero.estaAprobado()) {
            throw new ErrorLogicaNegocioException(
                    "El semillero aún no ha sido aprobado por el administrador; no se puede cambiar su estado.");
        }

        Estado estado = buscadorEstados.porIdEnCategoria(CategoriasEstado.SEMILLERO, idEstado);
        if (!estado.tieneNombre(EstadosSemillero.ACTIVO) && !estado.tieneNombre(EstadosSemillero.INACTIVO)) {
            throw new ErrorLogicaNegocioException("Un semillero aprobado solo puede estar 'Activo' o 'Inactivo'.");
        }
        if (!EstadosSemillero.ACTIVO.equalsIgnoreCase(estado.getNombre()) && tieneConvocatoriaAbierta(idSemillero)) {
            throw new ErrorLogicaNegocioException(
                    "Cierra o cancela las convocatorias abiertas antes de cambiar el semillero a '" + estado.getNombre() + "'.");
        }

        semillero.setEstado(estado);
        Semillero semilleroActualizado = semilleroRepository.save(semillero);
        return semilleroMapper.aResponse(semilleroActualizado);
    }

    /**
     * Elimina un semillero por su ID
     */
    public void eliminar(Integer id) {
        publicarCambioSemilleros();
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
        publicarCambioSemilleros();
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

        return semilleroMapper.aLinea(semilleroLineaRepository.save(asociacion));
    }

    public List<SemilleroLineaInvestigacionResponseDTO> obtenerLineasInvestigacion(Integer idSemillero) {
        return semilleroLineaRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(semilleroMapper::aLinea)
                .toList();
    }

    public void eliminarLineaInvestigacion(Integer idSemillero, Integer idLinea) {
        publicarCambioSemilleros();
        SemilleroLineaInvestigacion asociacion = semilleroLineaRepository.findBySemilleroIdAndLineaInvestigacionId(idSemillero, idLinea)
                .orElseThrow(() -> new RecursoNoEncontradoException("SemilleroLineaInvestigacion", "idLinea", idLinea));
        if (semilleroLineaRepository.findBySemilleroId(idSemillero).size() <= 1) {
            throw new ErrorLogicaNegocioException("El semillero debe conservar al menos una línea de investigación.");
        }
        semilleroLineaRepository.delete(asociacion);
    }

    private boolean tieneConvocatoriaAbierta(Integer idSemillero) {
        return convocatoriaRepository.findBySemilleroId(idSemillero).stream()
                .anyMatch(Convocatoria::estaAbierta);
    }

    // Invalida el árbol KD de semilleros similares. El índice escucha el evento después del
    // commit: si la operación falla y se revierte, el árbol no se descarta.
    private void publicarCambioSemilleros() {
        eventos.publishEvent(new SemillerosModificadosEvent());
    }
}
