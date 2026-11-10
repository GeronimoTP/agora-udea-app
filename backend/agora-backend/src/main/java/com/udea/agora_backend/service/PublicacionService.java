package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.PublicacionRequestDTO;
import com.udea.agora_backend.dto.request.PublicacionUsuarioRequestDTO;
import com.udea.agora_backend.dto.response.PublicacionResponseDTO;
import com.udea.agora_backend.dto.response.PublicacionUsuarioResponseDTO;
import com.udea.agora_backend.exception.ConflictoException;
import com.udea.agora_backend.exception.ErrorLogicaNegocioException;
import com.udea.agora_backend.exception.RecursoNoEncontradoException;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Publicacion;
import com.udea.agora_backend.model.PublicacionUsuario;
import com.udea.agora_backend.model.RolAutor;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteSemilleroRepository;
import com.udea.agora_backend.repository.ProfesorSemilleroRepository;
import com.udea.agora_backend.repository.ProyectoRepository;
import com.udea.agora_backend.repository.PublicacionRepository;
import com.udea.agora_backend.repository.PublicacionUsuarioRepository;
import com.udea.agora_backend.repository.RolAutorRepository;
import com.udea.agora_backend.repository.UsuarioRepository;
import com.udea.agora_backend.service.mapper.PublicacionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de publicaciones y producción académica.
 * Maneja operaciones CRUD y asignación de autores con roles a cada publicación.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolAutorRepository rolAutorRepository;
    private final PublicacionUsuarioRepository publicacionUsuarioRepository;
    private final EstudianteSemilleroRepository estudianteSemilleroRepository;
    private final ProfesorSemilleroRepository profesorSemilleroRepository;
    private final PublicacionMapper publicacionMapper;

    /**
     * Obtiene todas las publicaciones
     */
    public List<PublicacionResponseDTO> obtenerTodas() {
        return publicacionRepository.findAll()
                .stream()
                .map(publicacionMapper::aResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una publicación por su ID
     */
    public PublicacionResponseDTO obtenerPorId(Integer id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));
        return publicacionMapper.aResponse(publicacion);
    }

    /**
     * Obtiene publicaciones por proyecto
     */
    public List<PublicacionResponseDTO> obtenerPorSemillero(Integer idSemillero) {
        return publicacionRepository.findByProyectoSemilleroId(idSemillero)
                .stream()
                .map(publicacionMapper::aResponse)
                .collect(Collectors.toList());
    }

    public List<PublicacionResponseDTO> obtenerPorProyecto(Integer idProyecto) {
        return publicacionRepository.findByProyectoId(idProyecto)
                .stream()
                .map(publicacionMapper::aResponse)
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva publicación
     */
    public PublicacionResponseDTO crear(PublicacionRequestDTO request) {
        Proyecto proyecto = proyectoRepository.findById(request.getIdProyecto())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", request.getIdProyecto()));

        Publicacion publicacion = Publicacion.builder()
                .titulo(request.getTitulo())
                .urlDocumento(request.getUrlDocumento())
                .fechaPublicacion(request.getFechaPublicacion())
                .proyecto(proyecto)
                .createdAt(ZonedDateTime.now())
                .build();

        Publicacion guardada = publicacionRepository.save(publicacion);
        return publicacionMapper.aResponse(guardada);
    }

    /**
     * Actualiza una publicación existente
     */
    public PublicacionResponseDTO actualizar(Integer id, PublicacionRequestDTO request) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));

        // Puede moverse a otro proyecto, pero solo dentro del mismo semillero
        if (!publicacion.getProyecto().getId().equals(request.getIdProyecto())) {
            Proyecto proyecto = proyectoRepository.findById(request.getIdProyecto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", request.getIdProyecto()));
            if (!proyecto.getSemillero().getId().equals(publicacion.getProyecto().getSemillero().getId())) {
                throw new ErrorLogicaNegocioException("Una publicación no puede moverse a un proyecto de otro semillero.");
            }
            publicacion.setProyecto(proyecto);
        }

        publicacion.setTitulo(request.getTitulo());
        publicacion.setUrlDocumento(request.getUrlDocumento());
        publicacion.setFechaPublicacion(request.getFechaPublicacion());

        Publicacion actualizada = publicacionRepository.save(publicacion);
        return publicacionMapper.aResponse(actualizada);
    }

    /**
     * Elimina una publicación por su ID
     */
    public void eliminar(Integer id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));
        publicacionUsuarioRepository.deleteByPublicacionId(id);
        publicacionRepository.delete(publicacion);
    }

    // ==========================================
    // GESTIÓN DE AUTORES DE LA PUBLICACIÓN
    // ==========================================

    public PublicacionUsuarioResponseDTO asociarAutor(PublicacionUsuarioRequestDTO request) {
        Publicacion publicacion = publicacionRepository.findById(request.getIdPublicacion())
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", request.getIdPublicacion()));

        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", request.getIdUsuario()));

        if (!perteneceAlSemillero(usuario, publicacion.getProyecto().getSemillero())) {
            throw new ErrorLogicaNegocioException(
                    "Solo el líder, los integrantes activos o los tutores del semillero pueden ser autores.");
        }

        RolAutor rolAutor = rolAutorRepository.findById(request.getIdRolAutor())
                .orElseThrow(() -> new RecursoNoEncontradoException("RolAutor", request.getIdRolAutor()));

        if (publicacionUsuarioRepository.findByPublicacionIdAndUsuarioId(request.getIdPublicacion(), request.getIdUsuario()).isPresent()) {
            throw new ConflictoException("PublicacionUsuario", "idUsuario", request.getIdUsuario().toString());
        }

        PublicacionUsuario asociacion = PublicacionUsuario.builder()
                .publicacion(publicacion)
                .usuario(usuario)
                .rolAutor(rolAutor)
                .fechaAsociacion(LocalDate.now())
                .build();

        PublicacionUsuario guardada = publicacionUsuarioRepository.save(asociacion);

        return publicacionMapper.aAutoria(guardada);
    }

    public List<PublicacionUsuarioResponseDTO> obtenerAutores(Integer idPublicacion) {
        return publicacionUsuarioRepository.findByPublicacionId(idPublicacion)
                .stream()
                .map(publicacionMapper::aAutoria)
                .collect(Collectors.toList());
    }

    public void removerAutor(Integer idPublicacion, Integer idUsuario) {
        PublicacionUsuario asociacion = publicacionUsuarioRepository.findByPublicacionIdAndUsuarioId(idPublicacion, idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("PublicacionUsuario", "idUsuario", idUsuario));
        publicacionUsuarioRepository.delete(asociacion);
    }

    private boolean perteneceAlSemillero(Usuario usuario, Semillero semillero) {
        Integer idUsuario = usuario.getId();
        if (semillero.getEstudianteLider().getUsuario().getId().equals(idUsuario)) {
            return true;
        }
        return estudianteSemilleroRepository.findBySemilleroIdAndFechaSalidaIsNull(semillero.getId()).stream()
                        .anyMatch(es -> es.getEstudiante().getUsuario().getId().equals(idUsuario))
                || profesorSemilleroRepository.findBySemilleroId(semillero.getId()).stream()
                        .anyMatch(ps -> ps.getProfesor().getUsuario().getId().equals(idUsuario));
    }
}
