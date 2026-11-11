package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.CategoriasEstado;
import com.udea.agora_backend.dominio.EstadosSolicitud;
import com.udea.agora_backend.dominio.EstadosVinculacion;
import com.udea.agora_backend.dto.request.EstudianteSemilleroRequestDTO;
import com.udea.agora_backend.dto.request.ProfesorSemilleroRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorSemilleroResponseDTO;
import com.udea.agora_backend.dto.response.SemilleroResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteSemillero;
import com.udea.agora_backend.model.Profesor;
import com.udea.agora_backend.model.ProfesorSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.ProfesorRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.repository.SolicitudRetiroRepository;
import com.udea.agora_backend.service.mapper.SemilleroMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Personas de un semillero: profesores tutores (co-tutoría), estudiantes integrantes
 * (vinculación, retiro) y el liderazgo estudiantil (transferencia). Los datos propios del
 * semillero (CRUD, estado, líneas) están en {@link SemilleroService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MiembrosSemilleroService {

    private final SemilleroRepository semilleroRepository;
    private final EstudianteRepository estudianteRepository;
    private final ProfesorRepository profesorRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final BuscadorEstados buscadorEstados;
    private final LiderazgoSemilleroService liderazgoSemilleroService;
    private final SemilleroMapper semilleroMapper;
    private final SolicitudRetiroRepository solicitudRetiroRepository;

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

        return semilleroMapper.aTutor(profesorSemilleroRepository.save(asociacion));
    }

    public List<ProfesorSemilleroResponseDTO> obtenerProfesores(Integer idSemillero) {
        return profesorSemilleroRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(semilleroMapper::aTutor)
                .toList();
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

        Estado estado = buscadorEstados.porIdEnCategoria(CategoriasEstado.VINCULACION, request.getIdEstado());
        validarAprobado(semillero);

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

        return semilleroMapper.aVinculacion(estudianteSemilleroRepository.save(vinculacion));
    }

    /**
     * Un estudiante que acepta una oferta pasa a ser integrante del semillero. Si había sido
     * retirado antes, su vinculación se reactiva con fecha de ingreso de hoy.
     */
    public void incorporarAdmitido(Estudiante estudiante, Semillero semillero) {
        Optional<EstudianteSemillero> existente = estudianteSemilleroRepository
                .findBySemilleroIdAndEstudianteId(semillero.getId(), estudiante.getId());
        if (existente.isPresent()) {
            EstudianteSemillero vinculacion = existente.get();
            if (!vinculacion.estaActiva()) {
                vinculacion.setFechaSalida(null);
                vinculacion.setFechaIngreso(LocalDate.now());
                vinculacion.setEstado(buscadorEstados.deVinculacion(EstadosVinculacion.ACTIVA));
                estudianteSemilleroRepository.save(vinculacion);
                log.info("Estudiante {} reincorporado al semillero {}", estudiante.getId(), semillero.getId());
            }
            return;
        }
        estudianteSemilleroRepository.save(EstudianteSemillero.builder()
                .semillero(semillero)
                .estudiante(estudiante)
                .fechaIngreso(LocalDate.now())
                .estado(buscadorEstados.deVinculacion(EstadosVinculacion.ACTIVA))
                .build());
        log.info("Estudiante {} vinculado al semillero {}", estudiante.getId(), semillero.getId());
    }

    public List<EstudianteSemilleroResponseDTO> obtenerEstudiantes(Integer idSemillero) {
        return estudianteSemilleroRepository.findBySemilleroId(idSemillero)
                .stream()
                .map(semilleroMapper::aVinculacion)
                .toList();
    }

    /** Solo integrantes activos (sin fecha de salida): lo que ven los compañeros de equipo. */
    public List<EstudianteSemilleroResponseDTO> obtenerIntegrantesActivos(Integer idSemillero) {
        return estudianteSemilleroRepository.findBySemilleroIdAndFechaSalidaIsNull(idSemillero)
                .stream()
                .map(semilleroMapper::aVinculacion)
                .toList();
    }

    public EstudianteSemilleroResponseDTO actualizarVinculacionEstudiante(Integer idSemillero, Integer idEstudiante, Integer idEstado, LocalDate fechaSalida) {
        EstudianteSemillero vinculacion = estudianteSemilleroRepository.findBySemilleroIdAndEstudianteId(idSemillero, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteSemillero", "idEstudiante", idEstudiante));

        vinculacion.setEstado(buscadorEstados.porIdEnCategoria(CategoriasEstado.VINCULACION, idEstado));
        if (fechaSalida != null) {
            vinculacion.setFechaSalida(fechaSalida);
        }

        return semilleroMapper.aVinculacion(estudianteSemilleroRepository.save(vinculacion));
    }

    /**
     * Retira a un integrante: la vinculación se conserva como historial con fecha de salida
     * y estado "Inactivo". Un integrante retirado puede volver a postularse.
     */
    public EstudianteSemilleroResponseDTO retirarEstudiante(Integer idSemillero, Integer idEstudiante) {
        EstudianteSemillero vinculacion = estudianteSemilleroRepository.findBySemilleroIdAndEstudianteId(idSemillero, idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteSemillero", "idEstudiante", idEstudiante));
        if (!vinculacion.estaActiva()) {
            throw new ErrorLogicaNegocioException("El estudiante ya fue retirado del semillero.");
        }
        vinculacion.setFechaSalida(LocalDate.now());
        vinculacion.setEstado(buscadorEstados.deVinculacion(EstadosVinculacion.INACTIVA));
        resolverSolicitudesDeRetiroPendientes(vinculacion);
        return semilleroMapper.aVinculacion(estudianteSemilleroRepository.save(vinculacion));
    }

    /**
     * Transfiere el liderazgo estudiantil a un integrante activo. El líder saliente queda
     * como integrante activo del semillero, para no perder su historial.
     */
    public SemilleroResponseDTO transferirLiderazgo(Integer idSemillero, Integer idNuevoLider) {
        Semillero semillero = semilleroRepository.findById(idSemillero)
                .orElseThrow(() -> new RecursoNoEncontradoException("Semillero", idSemillero));
        validarAprobado(semillero);
        Estudiante liderActual = semillero.getEstudianteLider();
        if (liderActual.getId().equals(idNuevoLider)) {
            throw new ErrorLogicaNegocioException("El estudiante ya es el líder del semillero.");
        }
        if (!estudianteSemilleroRepository.existsBySemilleroIdAndEstudianteIdAndFechaSalidaIsNull(idSemillero, idNuevoLider)) {
            throw new ErrorLogicaNegocioException("El nuevo líder debe ser un integrante activo del semillero.");
        }
        liderazgoSemilleroService.validarQueNoLideraOtroSemillero(idNuevoLider);
        Estudiante nuevoLider = estudianteRepository.findById(idNuevoLider)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", idNuevoLider));

        Estado activo = buscadorEstados.deVinculacion(EstadosVinculacion.ACTIVA);
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
        return semilleroMapper.aResponse(semilleroRepository.save(semillero));
    }

    // Si el líder retira directamente a quien había pedido salir, su solicitud queda aprobada
    private void resolverSolicitudesDeRetiroPendientes(EstudianteSemillero vinculacion) {
        solicitudRetiroRepository.findByVinculacionYEstado(vinculacion.getId(), EstadosSolicitud.PENDIENTE)
                .forEach(solicitud -> {
                    solicitud.setEstado(buscadorEstados.deSolicitud(EstadosSolicitud.APROBADA));
                    solicitud.setFechaRespuesta(ZonedDateTime.now());
                    if (solicitud.getComentarioRespuesta() == null) {
                        solicitud.setComentarioRespuesta("Retirado por un líder del semillero.");
                    }
                    solicitudRetiroRepository.save(solicitud);
                });
    }

    // Un semillero sin aprobar no tiene integrantes: solo sus líderes lo gestionan
    private static void validarAprobado(Semillero semillero) {
        if (!semillero.estaAprobado()) {
            throw new ErrorLogicaNegocioException("El semillero aún no ha sido aprobado por el administrador.");
        }
    }
}
