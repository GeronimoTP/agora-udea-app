package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.PostulacionHabilidadRequestDTO;
import com.udea.agora_backend.dto.request.PostulacionRequestDTO;
import com.udea.agora_backend.dto.response.PostulacionHabilidadResponseDTO;
import com.udea.agora_backend.dto.response.PostulacionResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final EstadoRepository estadoRepository;
    private final HabilidadRepository habilidadRepository;
    private final PostulacionHabilidadRepository postulacionHabilidadRepository;

    /**
     * Obtiene todas las postulaciones
     */
    public List<PostulacionResponseDTO> obtenerTodas() {
        return postulacionRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una postulación por su ID
     */
    public PostulacionResponseDTO obtenerPorId(Integer id) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));
        return mapeoAResponseDTO(postulacion);
    }

    /**
     * Obtiene postulaciones de un estudiante
     */
    public List<PostulacionResponseDTO> obtenerPorEstudiante(Integer idEstudiante) {
        return postulacionRepository.findByEstudianteId(idEstudiante)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene postulaciones de una convocatoria
     */
    public List<PostulacionResponseDTO> obtenerPorConvocatoria(Integer idConvocatoria) {
        return postulacionRepository.findByConvocatoriaId(idConvocatoria)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene postulaciones de un estudiante por su estado
     */
    public List<PostulacionResponseDTO> obtenerPorEstudianteYEstado(Integer idEstudiante, Integer idEstado) {
        return postulacionRepository.findByEstudianteIdAndEstadoId(idEstudiante, idEstado)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene postulaciones activas de un estudiante (aún sin resolver: en
     * revisión del líder o esperando confirmación del estudiante)
     */
    public List<PostulacionResponseDTO> obtenerPostulacionesActivasByEstudiante(Integer idEstudiante) {
        return postulacionRepository.findPostulacionesActivasByEstudiante(idEstudiante, "Pendiente", "Pre-aprobada")
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene postulaciones de una convocatoria con estado especificado
     */
    public List<PostulacionResponseDTO> obtenerPorConvocatoriaYEstado(Integer idConvocatoria, String... estadoNombres) {
        return postulacionRepository.findPostulacionesByConvocatoriaAndEstado(idConvocatoria, estadoNombres)
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene postulaciones sin respuesta que han expirado
     */
    public List<PostulacionResponseDTO> obtenerPostulacionesSinRespuestaExpiradas() {
        return postulacionRepository.findPostulacionesSinRespuestaExpiradas()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
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

        // Obtener estado inicial (Pendiente)
        Estado estado = estadoRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase("Pendiente"))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Estado", "nombre", "Pendiente"));

        // Crear postulación
        Postulacion postulacion = Postulacion.builder()
                .estudiante(estudiante)
                .convocatoria(convocatoria)
                .respuestaMotivacion(request.getRespuestaMotivacion())
                .disponibilidadHorasSemana(request.getDisponibilidadHorasSemana())
                .experienciaPrevia(request.getExperienciaPrevia())
                .fechaPostulacion(LocalDate.now())
                .estado(estado)
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

        return mapeoAResponseDTO(postulacionGuardada);
    }

    /**
     * Actualiza una postulación existente
     */
    public PostulacionResponseDTO actualizar(Integer id, PostulacionRequestDTO request) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));

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

        return mapeoAResponseDTO(postulacionActualizada);
    }

    /**
     * Elimina una postulación por su ID
     */
    public void eliminar(Integer id) {
        Postulacion postulacion = postulacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", id));
        postulacionRepository.delete(postulacion);
    }

    // ==========================================
    // HABILIDADES DECLARADAS EN LA POSTULACIÓN
    // ==========================================

    public PostulacionHabilidadResponseDTO agregarHabilidad(PostulacionHabilidadRequestDTO request) {
        Postulacion postulacion = postulacionRepository.findById(request.getIdPostulacion())
                .orElseThrow(() -> new RecursoNoEncontradoException("Postulación", request.getIdPostulacion()));

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

        return PostulacionHabilidadResponseDTO.builder()
                .id(guardada.getId())
                .idPostulacion(postulacion.getId())
                .nombreHabilidad(habilidad.getNombre())
                .build();
    }

    public List<PostulacionHabilidadResponseDTO> obtenerHabilidades(Integer idPostulacion) {
        return postulacionHabilidadRepository.findByPostulacionId(idPostulacion)
                .stream()
                .map(ph -> PostulacionHabilidadResponseDTO.builder()
                        .id(ph.getId())
                        .idPostulacion(ph.getPostulacion().getId())
                        .nombreHabilidad(ph.getHabilidad().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    public void eliminarHabilidad(Integer idPostulacion, Integer idHabilidad) {
        PostulacionHabilidad asociacion = postulacionHabilidadRepository.findByPostulacionIdAndHabilidadId(idPostulacion, idHabilidad)
                .orElseThrow(() -> new RecursoNoEncontradoException("PostulacionHabilidad", "idHabilidad", idHabilidad));
        postulacionHabilidadRepository.delete(asociacion);
    }

    /**
     * Mapea entidad Postulacion a ResponseDTO
     */
    private PostulacionResponseDTO mapeoAResponseDTO(Postulacion postulacion) {
        List<String> habilidades = postulacionHabilidadRepository.findByPostulacionId(postulacion.getId())
                .stream()
                .map(ph -> ph.getHabilidad().getNombre())
                .collect(Collectors.toList());

        return PostulacionResponseDTO.builder()
                .id(postulacion.getId())
                .nombreEstudiante(postulacion.getEstudiante() != null && postulacion.getEstudiante().getUsuario() != null ? postulacion.getEstudiante().getUsuario().getNombreCompleto() : null)
                .tituloConvocatoria(postulacion.getConvocatoria() != null ? postulacion.getConvocatoria().getTitulo() : null)
                .respuestaMotivacion(postulacion.getRespuestaMotivacion())
                .disponibilidadHorasSemana(postulacion.getDisponibilidadHorasSemana())
                .experienciaPrevia(postulacion.getExperienciaPrevia())
                .fechaPostulacion(postulacion.getFechaPostulacion())
                .fechaDecisionEstudiante(postulacion.getFechaDecisionEstudiante())
                .estadoPostulacion(postulacion.getEstado() != null ? postulacion.getEstado().getNombre() : null)
                .habilidades(habilidades)
                .build();
    }
}
