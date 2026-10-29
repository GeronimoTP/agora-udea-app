package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.EstudianteLineaInvestigacionRequestDTO;
import com.udea.agora_backend.dto.request.EstudianteRequestDTO;
import com.udea.agora_backend.dto.response.EstudianteLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.EstudianteResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.*;
import com.udea.agora_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
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
        private final RolRepository rolRepository;
        private final HabilidadRepository habilidadRepository;
        private final EstudianteHabilidadRepository estudianteHabilidadRepository;

        /**
         * Obtiene todos los estudiantes
         */
        public List<EstudianteResponseDTO> obtenerTodos() {
                return estudianteRepository.findAll()
                                .stream()
                                .map(this::mapeoAResponseDTO)
                                .collect(Collectors.toList());
        }

        /**
         * Obtiene un estudiante por su ID
         */
        public EstudianteResponseDTO obtenerPorId(Integer id) {
                Estudiante estudiante = estudianteRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", id));
                return mapeoAResponseDTO(estudiante);
        }

        /**
         * Obtiene un estudiante por ID de usuario
         */
        public EstudianteResponseDTO obtenerPorIdUsuario(Integer idUsuario) {
                Estudiante estudiante = estudianteRepository.findByUsuarioId(idUsuario)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante", "usuarioId",
                                                idUsuario));
                return mapeoAResponseDTO(estudiante);
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

                return mapeoAResponseDTO(estudianteGuardado);
        }

        /**
         * Crea un nuevo estudiante junto con su usuario (para registro desde frontend)
         * Este método NO necesita que el usuario ya exista
         * 
         * @param email                 Email del usuario
         * @param nombreCompleto        Nombre completo
         * @param idPrograma            ID del programa académico
         * @param semestre              Semestre del estudiante
         * @param idLineasInvestigacion IDs de líneas de investigación de interés
         * @return El estudiante creado
         */
        public EstudianteResponseDTO registrarEstudiante(
                        String email,
                        String nombreCompleto,
                        Integer idPrograma,
                        Integer semestre,
                        List<Integer> idLineasInvestigacion) {

                // Obtener programa
                ProgramaAcademico programa = programaRepository.findById(idPrograma)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", idPrograma));

                // Obtener rol de estudiante
                Rol rolEstudiante = rolRepository.findByNombre("Estudiante")
                                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", "nombre", "Estudiante"));

                // Crear usuario
                Usuario usuario = new Usuario();
                usuario.setNombreCompleto(nombreCompleto);
                usuario.setEmail(email);
                usuario.setRol(rolEstudiante);
                usuario = usuarioRepository.save(usuario);

                // Crear estudiante
                Estudiante estudiante = Estudiante.builder()
                                .usuario(usuario)
                                .programa(programa)
                                .semestre(semestre)
                                .createdAt(ZonedDateTime.now())
                                .build();

                Estudiante estudianteGuardado = estudianteRepository.save(estudiante);

                // Guardar líneas de investigación
                if (idLineasInvestigacion != null && !idLineasInvestigacion.isEmpty()) {
                        for (Integer idLinea : idLineasInvestigacion) {
                                LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                                "Línea de Investigación", idLinea));

                                EstudianteLineaInvestigacion eli = EstudianteLineaInvestigacion.builder()
                                                .estudiante(estudianteGuardado)
                                                .lineaInvestigacion(linea)
                                                .build();
                                estudianteLineaRepository.save(eli);
                        }
                }

                return mapeoAResponseDTO(estudianteGuardado);
        }

        /**
         * Sobrecarga de crear para registrar estudiante junto a su usuario desde AuthController
         */
        public Estudiante crear(
                        String email,
                        String nombreCompleto,
                        Integer idPrograma,
                        Integer semestre,
                        List<Integer> idLineasInvestigacion) {
                // Obtener programa
                ProgramaAcademico programa = programaRepository.findById(idPrograma)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Programa Académico", idPrograma));

                // Obtener rol de estudiante
                Rol rolEstudiante = rolRepository.findByNombre("Estudiante")
                                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", "nombre", "Estudiante"));

                // Crear usuario
                Usuario usuario = new Usuario();
                usuario.setNombreCompleto(nombreCompleto);
                usuario.setEmail(email);
                usuario.setRol(rolEstudiante);
                usuario = usuarioRepository.save(usuario);

                // Crear estudiante
                Estudiante estudiante = Estudiante.builder()
                                .usuario(usuario)
                                .programa(programa)
                                .semestre(semestre)
                                .createdAt(ZonedDateTime.now())
                                .build();

                Estudiante estudianteGuardado = estudianteRepository.save(estudiante);

                // Guardar líneas de investigación
                if (idLineasInvestigacion != null && !idLineasInvestigacion.isEmpty()) {
                        for (Integer idLinea : idLineasInvestigacion) {
                                LineaInvestigacion linea = lineaInvestigacionRepository.findById(idLinea)
                                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                                "Línea de Investigación", idLinea));

                                EstudianteLineaInvestigacion eli = EstudianteLineaInvestigacion.builder()
                                                .estudiante(estudianteGuardado)
                                                .lineaInvestigacion(linea)
                                                .build();
                                estudianteLineaRepository.save(eli);
                        }
                }

                return estudianteGuardado;
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

                return mapeoAResponseDTO(estudianteActualizado);
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

                return EstudianteLineaInvestigacionResponseDTO.builder()
                                .id(guardada.getId())
                                .nombreEstudiante(estudiante.getUsuario().getNombreCompleto())
                                .nombreLineaInvestigacion(linea.getNombre())
                                .build();
        }

        public List<EstudianteLineaInvestigacionResponseDTO> obtenerLineasInvestigacion(Integer idEstudiante) {
                return estudianteLineaRepository.findByEstudianteId(idEstudiante)
                                .stream()
                                .map(eli -> EstudianteLineaInvestigacionResponseDTO.builder()
                                                .id(eli.getId())
                                                .nombreEstudiante(eli.getEstudiante().getUsuario().getNombreCompleto())
                                                .nombreLineaInvestigacion(eli.getLineaInvestigacion().getNombre())
                                                .build())
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
         * Mapea entidad Estudiante a ResponseDTO
         */
        /**
         * Reemplaza las habilidades del perfil del estudiante. Las usa el motor de
         * recomendación desde el registro, sin esperar a la primera postulación.
         */
        public void reemplazarHabilidades(Estudiante estudiante, List<Integer> idHabilidades) {
                // Solo se borran las que salen y se insertan las nuevas: Hibernate ejecuta los
                // INSERT antes que los DELETE y la tabla tiene UNIQUE (estudiante, habilidad)
                java.util.Set<Integer> nuevas = new java.util.LinkedHashSet<>(idHabilidades);
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

        private EstudianteResponseDTO mapeoAResponseDTO(Estudiante estudiante) {
                List<String> lineas = estudianteLineaRepository.findByEstudianteId(estudiante.getId())
                                .stream()
                                .map(eli -> eli.getLineaInvestigacion().getNombre())
                                .collect(Collectors.toList());

                return EstudianteResponseDTO.builder()
                                .id(estudiante.getId())
                                .nombreCompleto(estudiante.getUsuario() != null
                                                ? estudiante.getUsuario().getNombreCompleto()
                                                : null)
                                .email(estudiante.getUsuario() != null ? estudiante.getUsuario().getEmail() : null)
                                .nombrePrograma(estudiante.getPrograma() != null ? estudiante.getPrograma().getNombre()
                                                : null)
                                .semestre(estudiante.getSemestre())
                                .lineasInvestigacion(lineas)
                                .habilidades(estudianteHabilidadRepository.findByEstudianteId(estudiante.getId())
                                                .stream()
                                                .map(eh -> eh.getHabilidad().getNombre())
                                                .collect(Collectors.toList()))
                                .createdAt(estudiante.getCreatedAt())
                                .build();
        }
}
