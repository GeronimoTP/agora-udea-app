package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.EstudianteLineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.EstudianteRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.EstudianteResponseDTO;
import com.udea.agora_backend.dto.response.EstudianteResumenDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteHabilidad;
import com.udea.agora_backend.model.EstudianteLineaInvestigacion;
import com.udea.agora_backend.model.Habilidad;
import com.udea.agora_backend.model.LineaInvestigacion;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteHabilidadRepository;
import com.udea.agora_backend.repository.EstudianteLineaInvestigacionRepository;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.HabilidadRepository;
import com.udea.agora_backend.repository.LineaInvestigacionRepository;
import com.udea.agora_backend.repository.ProgramaAcademicoRepository;
import com.udea.agora_backend.repository.SemilleroRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.service.mapper.EstudianteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de estudiantes.
 * Maneja operaciones CRUD, búsquedas y asignación de líneas de
 * interés/investigación.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final LineaInvestigacionRepository lineaInvestigacionRepository;
    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final HabilidadRepository habilidadRepository;
    private final EstudianteHabilidadRepository estudianteHabilidadRepository;
    private final EstudianteMapper estudianteMapper;
    private final SemilleroRepository semilleroRepository;

    /**
    * Obtiene todos los estudiantes
    */
    public List<EstudianteResponseDTO> obtenerTodos() {
        return estudianteMapper.aResponses(estudianteRepository.findAll());
    }

    /** Listado sin datos personales para elegir al líder de un semillero (ver {@link EstudianteResumenDTO}). */
    public List<EstudianteResumenDTO> obtenerResumenes() {
        Set<Integer> lideres = new HashSet<>(semilleroRepository.findIdsEstudiantesLideres());
        return estudianteRepository.findAll().stream()
                .map(e -> EstudianteResumenDTO.builder()
                        .id(e.getId())
                        .nombreCompleto(e.getUsuario().getNombreCompleto())
                        .nombrePrograma(e.getPrograma() != null ? e.getPrograma().getNombre() : null)
                        .semestre(e.getSemestre())
                        .lideraSemillero(lideres.contains(e.getId()))
                        .build())
                .toList();
    }

    /**
    * Obtiene un estudiante por su ID
    */
    public EstudianteResponseDTO obtenerPorId(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));
        return estudianteMapper.aResponse(estudiante);
    }

    /**
    * Obtiene un estudiante por ID de usuario
    */
    public EstudianteResponseDTO obtenerPorIdUsuario(Integer idUsuario) {
        Estudiante estudiante = estudianteRepository.findByUsuarioId(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", "usuarioId",
                        idUsuario));
        return estudianteMapper.aResponse(estudiante);
    }

    /**
    * Crea un nuevo estudiante
    */
    public EstudianteResponseDTO crear(EstudianteRequestDTO request) {
        // Verificar usuario no duplicado
        if (estudianteRepository.findByUsuarioId(request.getIdUsuario()).isPresent()) {
            throw new ConflictoException("Estudiante", "usuarioId", request.getIdUsuario().toString());
        }

        // Obtener usuario
        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getIdUsuario()));

        // Obtener programa
        ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico",
                        request.getIdPrograma()));

        // Crear estudiante
        Estudiante estudiante = Estudiante.builder()
                .usuario(usuario)
                .programa(programa)
                .semestre(request.getSemestre())
                .createdAt(ZonedDateTime.now())
                .build();

        Estudiante estudianteGuardado = estudianteRepository.save(estudiante);

        // Guardar líneas de investigación iniciales
        if (request.getIdLineasInvestigacion() != null && !request.getIdLineasInvestigacion().isEmpty()) {
            for (Integer idLinea : request.getIdLineasInvestigacion()) {
                LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "LineaInvestigacion", idLinea));

                EstudianteLineaInvestigacion eli = EstudianteLineaInvestigacion.builder()
                        .estudiante(estudianteGuardado)
                        .lineaInvestigacion(linea)
                        .build();
                estudianteLineaRepository.save(eli);
            }
        }

        reemplazarHabilidades(estudianteGuardado, request.getIdHabilidades());

        return estudianteMapper.aResponse(estudianteGuardado);
    }

    /**
    * Actualiza un estudiante existente
    */
    public EstudianteResponseDTO actualizar(Integer id, EstudianteRequestDTO request) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));

        // Obtener programa si cambió
        if (!estudiante.getPrograma().getId().equals(request.getIdPrograma())) {
            ProgramaAcademico programa = programaRepository.findById(request.getIdPrograma())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico",
                            request.getIdPrograma()));
            estudiante.setPrograma(programa);
        }

        // Actualizar semestre
        estudiante.setSemestre(request.getSemestre());

        Estudiante estudianteActualizado = estudianteRepository.save(estudiante);

        // Sincronizar líneas de investigación si vienen en el request
        if (request.getIdLineasInvestigacion() != null) {
            // Eliminar anteriores
            List<EstudianteLineaInvestigacion> anteriores = estudianteLineaRepository
                    .findByEstudianteId(id);
            estudianteLineaRepository.deleteAll(anteriores);

            // Guardar nuevas
            for (Integer idLinea : request.getIdLineasInvestigacion()) {
                LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "LineaInvestigacion", idLinea));

                EstudianteLineaInvestigacion eli = EstudianteLineaInvestigacion.builder()
                        .estudiante(estudianteActualizado)
                        .lineaInvestigacion(linea)
                        .build();
                estudianteLineaRepository.save(eli);
            }
        }

        if (request.getIdHabilidades() != null) {
            reemplazarHabilidades(estudianteActualizado, request.getIdHabilidades());
        }

        return estudianteMapper.aResponse(estudianteActualizado);
    }

    /**
    * Elimina un estudiante por su ID
    */
    public void eliminar(Integer id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));
        // Las asociaciones del perfil no tienen borrado en cascada en la BD
        estudianteHabilidadRepository.deleteAll(estudianteHabilidadRepository.findByEstudianteId(id));
        estudianteLineaRepository.deleteAll(estudianteLineaRepository.findByEstudianteId(id));
        estudianteRepository.delete(estudiante);
    }

    // ==========================================
    // LÍNEAS DE INVESTIGACIÓN / INTERÉS
    // ==========================================

    public EstudianteLineaInvestigacionResponseDTO agregarLineaInvestigacion(
            EstudianteLineaInvestigacionRequestDTO request) {
        Estudiante estudiante = estudianteRepository.findById(request.getIdEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante",
                        request.getIdEstudiante()));

        LineaInvestigacion linea = lineaInvestigacionRepository.findById(request.getIdLineaInvestigacion())
                .orElseThrow(() -> new RecursoNoEncontradoException("LineaInvestigacion",
                        request.getIdLineaInvestigacion()));

        if (estudianteLineaRepository.findByEstudianteIdAndLineaInvestigacionId(request.getIdEstudiante(),
                request.getIdLineaInvestigacion()).isPresent()) {
            throw new ConflictoException("EstudianteLineaInvestigacion", "idLineaInvestigacion",
                    request.getIdLineaInvestigacion().toString());
        }

        EstudianteLineaInvestigacion asociacion = EstudianteLineaInvestigacion.builder()
                .estudiante(estudiante)
                .lineaInvestigacion(linea)
                .build();

        EstudianteLineaInvestigacion guardada = estudianteLineaRepository.save(asociacion);

        return estudianteMapper.aLinea(guardada);
    }

    public List<EstudianteLineaInvestigacionResponseDTO> obtenerLineasInvestigacion(Integer idEstudiante) {
        return estudianteLineaRepository.findByEstudianteId(idEstudiante)
                .stream()
                .map(estudianteMapper::aLinea)
                .collect(Collectors.toList());
    }

    public void eliminarLineaInvestigacion(Integer idEstudiante, Integer idLinea) {
        EstudianteLineaInvestigacion asociacion = estudianteLineaRepository
                .findByEstudianteIdAndLineaInvestigacionId(idEstudiante, idLinea)
                .orElseThrow(() -> new RecursoNoEncontradoException("EstudianteLineaInvestigacion",
                        "idLinea", idLinea));
        estudianteLineaRepository.delete(asociacion);
    }

    /**
    * Reemplaza las habilidades del perfil del estudiante. Las usa el motor de
    * recomendación desde el registro, sin esperar a la primera postulación.
    */
    public void reemplazarHabilidades(Estudiante estudiante, List<Integer> idHabilidades) {
        // Solo se borran las que salen y se insertan las nuevas: Hibernate ejecuta los
        // INSERT antes que los DELETE y la tabla tiene UNIQUE (estudiante, habilidad)
        Set<Integer> nuevas = new LinkedHashSet<>(idHabilidades);
        List<EstudianteHabilidad> actuales = estudianteHabilidadRepository.findByEstudianteId(estudiante.getId());
        estudianteHabilidadRepository.deleteAll(actuales.stream()
                .filter(eh -> !nuevas.contains(eh.getHabilidad().getId()))
                .toList());
        actuales.forEach(eh -> nuevas.remove(eh.getHabilidad().getId()));
        for (Integer idHabilidad : nuevas) {
            Habilidad habilidad = habilidadRepository.findById(idHabilidad)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Habilidad", idHabilidad));
            estudianteHabilidadRepository.save(EstudianteHabilidad.builder()
                    .estudiante(estudiante)
                    .habilidad(habilidad)
                    .build());
        }
    }
}
