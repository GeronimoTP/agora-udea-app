package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.PostulacionRequestDTO;
import com.udea.agora_backend.dto.response.PostulacionResponseDTO;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Postulacion;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.repository.PostulacionRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.ConvocatoriaRepository;
import com.udea.agora_backend.repository.EstadoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de postulaciones.
 * Maneja operaciones CRUD y búsquedas relacionadas con postulaciones de estudiantes.
 * La lógica de cascada de rechazos está en GestorConvocatoriasService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EstadoRepository estadoRepository;

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

    /**
     * Mapea entidad Postulacion a ResponseDTO
     */
    private PostulacionResponseDTO mapeoAResponseDTO(Postulacion postulacion) {
        return PostulacionResponseDTO.builder()
                .id(postulacion.getId())
                .nombreEstudiante(postulacion.getEstudiante().getUsuario().getNombreCompleto())
                .tituloConvocatoria(postulacion.getConvocatoria().getTitulo())
                .respuestaMotivacion(postulacion.getRespuestaMotivacion())
                .disponibilidadHorasSemana(postulacion.getDisponibilidadHorasSemana())
                .experienciaPrevia(postulacion.getExperienciaPrevia())
                .fechaPostulacion(postulacion.getFechaPostulacion())
                .fechaDecisionEstudiante(postulacion.getFechaDecisionEstudiante())
                .estadoPostulacion(postulacion.getEstado().getNombre())
                .build();
    }
}
