package com.udea.agora_backend.service;

import com.udea.agora_backend.dominio.EstadosPostulacion;
import com.udea.agora_backend.dto.request.PostulacionHabilidadRequestDTO;
import com.udea.agora_backend.dto.request.PostulacionRequestDTO;
import com.udea.agora_backend.dto.response.PostulacionHabilidadResponseDTO;
import com.udea.agora_backend.dto.response.PostulacionResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Habilidad;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.PostulacionHabilidad;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.HabilidadRepository;
import com.udea.agora_backend.repository.PostulacionHabilidadRepository;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.service.mapper.PostulacionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de postulaciones.
 * Maneja operaciones CRUD, búsquedas y asociación de habilidades a las postulaciones.
 * La lógica de cascada de rechazos y confirmación de doble vía está en GestorConvocatoriasService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final BuscadorEstados buscadorEstados;
    private final HabilidadRepository habilidadRepository;
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final PostulacionMapper postulacionMapper;
    private final RequisitosConvocatoriaService requisitosService;

    /**
     * Obtiene todas las postulaciones
     */
    public List<PostulacionResponseDTO> obtenerTodas() {
        return postulacionMapper.aResponses(postulacionRepository.findAll());
    }

    /**
     * Obtiene una postulación por su ID
     */
    public PostulacionResponseDTO obtenerPorId(Integer id) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));
        return postulacionMapper.aResponse(postulacion);
    }

    /**
     * Obtiene postulaciones de un estudiante
     */
    public List<PostulacionResponseDTO> obtenerPorEstudiante(Integer idEstudiante) {
        return postulacionMapper.aResponses(postulacionRepository.findByEstudianteId(idEstudiante));
    }

    public List<PostulacionResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return postulacionMapper.aResponses(postulacionRepository.findByConvocatoriaSemilleroId(idSemillero));
    }

    /**
     * Obtiene postulaciones de una convocatoria
     */
    public List<PostulacionResponseDTO> obtenerPorConvocatoria(Integer idConvocatoria) {
        return postulacionMapper.aResponses(postulacionRepository.findByConvocatoriaId(idConvocatoria));
    }

    /**
     * Obtiene postulaciones activas de un estudiante (aún sin resolver: en
     * revisión del líder o esperando confirmación del estudiante)
     */
    public List<PostulacionResponseDTO> obtenerPostulacionesActivasByEstudiante(Integer idEstudiante) {
        return postulacionMapper.aResponses(postulacionRepository.findPostulacionesActivasByEstudiante(
                idEstudiante, EstadosPostulacion.PENDIENTE, EstadosPostulacion.PRE_APROBADA));
    }

    /**
     * Crea una nueva postulación
     */
    public PostulacionResponseDTO crear(PostulacionRequestDTO request) {
        // Verificar postulación duplicada para la misma convocatoria
        if (postulacionRepository.findByConvocatoriaIdAndEstudianteId(request.getIdConvocatoria(), request.getIdEstudiante()).isPresent()) {
            throw new ConflictoException("Postulacion", "idConvocatoria y idEstudiante", request.getIdConvocatoria() + "-" + request.getIdEstudiante());
        }

        // Obtener estudiante
        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", request.getIdEstudiante()));

        // Obtener convocatoria
        Convocatoria convocatoria = convocatoriaRepository.findById(request.getIdConvocatoria())
                .orElseThrow(() -> new RecursoNoEncontradoException("Convocatoria", request.getIdConvocatoria()));

        validarConvocatoriaDisponible(convocatoria, estudiante);

        // Obtener estado inicial (Pendiente)
        Estado estado = buscadorEstados.dePostulacion(EstadosPostulacion.PENDIENTE);

        // Crear postulación
        Postulacion postulacion = Postulacion.builder()
                .estudiante(estudiante)
                .convocatoria(convocatoria)
                .respuestaMotivacion(request.getRespuestaMotivacion())
                .disponibilidadHorasSemana(request.getDisponibilidadHorasSemana())
                .experienciaPrevia(request.getExperienciaPrevia())
                .fechaPostulacion(LocalDate.now())
                .estado(estado)
                .createdAt(ZonedDateTime.now())
                .build();

        Postulacion postulacionGuardada = postulacionRepository.save(postulacion);

        // Guardar habilidades declaradas en la postulación
        if (request.getIdHabilidades() != null && !request.getIdHabilidades().isEmpty()) {
            for (Integer idHabilidad : request.getIdHabilidades()) {
                Habilidad habilidad = habilidadRepository.findById(idHabilidad)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", idHabilidad));

                PostulacionHabilidad ph = PostulacionHabilidad.builder()
                        .postulacion(postulacionGuardada)
                        .habilidad(habilidad)
                        .build();
                postulacionHabilidadRepository.save(ph);
            }
        }
        requisitosService.registrarDeclaraciones(postulacionGuardada, request.getIdRequisitosCumplidos());

        return postulacionMapper.aResponse(postulacionGuardada);
    }

    /**
     * Actualiza una postulación existente
     */
    public PostulacionResponseDTO actualizar(Integer id, PostulacionRequestDTO request) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));
        validarEditable(postulacion);

        // Actualizar campos
        postulacion.setRespuestaMotivacion(request.getRespuestaMotivacion());
        postulacion.setDisponibilidadHorasSemana(request.getDisponibilidadHorasSemana());
        postulacion.setExperienciaPrevia(request.getExperienciaPrevia());

        Postulacion postulacionActualizada = postulacionRepository.save(postulacion);

        // Sincronizar habilidades si vienen en el request
        if (request.getIdHabilidades() != null) {
            // Eliminar anteriores
            List<PostulacionHabilidad> anteriores = postulacionHabilidadRepository.findByPostulacionId(id);
            postulacionHabilidadRepository.deleteAll(anteriores);

            // Guardar nuevas
            for (Integer idHabilidad : request.getIdHabilidades()) {
                Habilidad habilidad = habilidadRepository.findById(idHabilidad)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", idHabilidad));

                PostulacionHabilidad ph = PostulacionHabilidad.builder()
                        .postulacion(postulacionActualizada)
                        .habilidad(habilidad)
                        .build();
                postulacionHabilidadRepository.save(ph);
            }
        }
        if (request.getIdRequisitosCumplidos() != null) {
            requisitosService.registrarDeclaraciones(postulacionActualizada, request.getIdRequisitosCumplidos());
        }

        return postulacionMapper.aResponse(postulacionActualizada);
    }

    /**
     * Elimina una postulación por su ID
     */
    public void eliminar(Integer id) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));
        // Solo se retira una postulación aún en revisión: nunca ocupó cupo ni recibió oferta,
        // así que eliminarla no altera los cupos de la convocatoria
        validarEditable(postulacion);
        postulacionHabilidadRepository.deleteAll(postulacionHabilidadRepository.findByPostulacionId(id));
        requisitosService.eliminarDeclaraciones(id);
        postulacionRepository.delete(postulacion);
    }

    // ==========================================
    // HABILIDADES DECLARADAS EN LA POSTULACIÓN
    // ==========================================

    public PostulacionHabilidadResponseDTO agregarHabilidad(PostulacionHabilidadRequestDTO request) {
        Postulacion postulacion = postulacionRepository.findById(request.getIdPostulacion())
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", request.getIdPostulacion()));
        validarEditable(postulacion);

        Habilidad habilidad = habilidadRepository.findById(request.getIdHabilidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", request.getIdHabilidad()));

        if (postulacionHabilidadRepository.findByPostulacionIdAndHabilidadId(request.getIdPostulacion(), request.getIdHabilidad()).isPresent()) {
            throw new ConflictoException("PostulacionHabilidad", "idHabilidad", request.getIdHabilidad().toString());
        }

        PostulacionHabilidad asociacion = PostulacionHabilidad.builder()
                .postulacion(postulacion)
                .habilidad(habilidad)
                .build();

        PostulacionHabilidad guardada = postulacionHabilidadRepository.save(asociacion);

        return postulacionMapper.aHabilidad(guardada);
    }

    public List<PostulacionHabilidadResponseDTO> obtenerHabilidades(Integer idPostulacion) {
        return postulacionHabilidadRepository.findByPostulacionId(idPostulacion)
                .stream()
                .map(postulacionMapper::aHabilidad)
                .collect(Collectors.toList());
    }

    public void eliminarHabilidad(Integer idPostulacion, Integer idHabilidad) {
        validarEditable(postulacionRepository.findById(idPostulacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", idPostulacion)));
        PostulacionHabilidad asociacion = postulacionHabilidadRepository.findByPostulacionIdAndHabilidadId(idPostulacion, idHabilidad)
                .orElseThrow(() -> new RecursoNoEncontradoException("PostulacionHabilidad", "idHabilidad", idHabilidad));
        postulacionHabilidadRepository.delete(asociacion);
    }

    // Reglas para postularse: convocatoria abierta, vigente, con cupos y de un semillero que el estudiante no lidera
    private void validarConvocatoriaDisponible(Convocatoria convocatoria, Estudiante estudiante) {
        if (!convocatoria.estaAbierta()) {
            throw new ErrorLogicaNegocioException("La convocatoria no está abierta para postulaciones.");
        }
        if (!convocatoria.tieneCuposDisponibles()) {
            throw new ErrorLogicaNegocioException("La convocatoria no tiene cupos disponibles.");
        }
        LocalDate hoy = LocalDate.now();
        if ((convocatoria.getFechaInicio() != null && hoy.isBefore(convocatoria.getFechaInicio()))
                || (convocatoria.getFechaCierre() != null && hoy.isAfter(convocatoria.getFechaCierre()))) {
            throw new ErrorLogicaNegocioException("La convocatoria está fuera de su periodo de postulación.");
        }
        if (convocatoria.getSemillero().getEstudianteLider().getId().equals(estudiante.getId())) {
            throw new ErrorLogicaNegocioException("No puedes postularte a una convocatoria de un semillero que lideras.");
        }
        // Un integrante retirado (con fecha de salida) puede volver a postularse
        if (estudianteSemilleroRepository.existsBySemilleroIdAndEstudianteIdAndFechaSalidaIsNull(
                convocatoria.getSemillero().getId(), estudiante.getId())) {
            throw new ErrorLogicaNegocioException("Ya perteneces a este semillero.");
        }
    }

    // Una postulación solo puede modificarse o retirarse mientras sigue en revisión del líder
    private void validarEditable(Postulacion postulacion) {
        if (!postulacion.tieneEstado(EstadosPostulacion.PENDIENTE)) {
            throw new ErrorLogicaNegocioException(
                    "Solo se puede modificar o retirar una postulación en estado 'Pendiente'. Estado actual: "
                            + postulacion.getEstado().getNombre());
        }
    }
}
