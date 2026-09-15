package com.udea.agora_backend.service;

import com.udea.agora_backend.dto.request.PublicacionRequestDTO;
import com.udea.agora_backend.dto.request.PublicacionUsuarioRequestDTO;
import com.udea.agora_backend.dto.response.PublicacionResponseDTO;
import com.udea.agora_backend.dto.response.PublicacionUsuarioResponseDTO;
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

    /**
     * Obtiene todas las publicaciones
     */
    public List<PublicacionResponseDTO> obtenerTodas() {
        return publicacionRepository.findAll()
                .stream()
                .map(this::mapeoAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una publicación por su ID
     */
    public PublicacionResponseDTO obtenerPorId(Integer id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));
        return mapeoAResponseDTO(publicacion);
    }

    /**
     * Obtiene publicaciones por proyecto
     */
    public List<PublicacionResponseDTO> obtenerPorProyecto(Integer idProyecto) {
        return publicacionRepository.findByProyectoId(idProyecto)
                .stream()
                .map(this::mapeoAResponseDTO)
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
        return mapeoAResponseDTO(guardada);
    }

    /**
     * Actualiza una publicación existente
     */
    public PublicacionResponseDTO actualizar(Integer id, PublicacionRequestDTO request) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));

        if (!publicacion.getProyecto().getId().equals(request.getIdProyecto())) {
            Proyecto proyecto = proyectoRepository.findById(request.getIdProyecto())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proyecto", request.getIdProyecto()));
            publicacion.setProyecto(proyecto);
        }

        publicacion.setTitulo(request.getTitulo());
        publicacion.setUrlDocumento(request.getUrlDocumento());
        publicacion.setFechaPublicacion(request.getFechaPublicacion());

        Publicacion actualizada = publicacionRepository.save(publicacion);
        return mapeoAResponseDTO(actualizada);
    }

    /**
     * Elimina una publicación por su ID
     */
    public void eliminar(Integer id) {
        Publicacion publicacion = publicacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Publicacion", id));
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

        return PublicacionUsuarioResponseDTO.builder()
                .id(guardada.getId())
                .nombrePublicacion(publicacion.getTitulo())
                .nombreUsuario(usuario.getNombreCompleto())
                .rolAutor(rolAutor.getNombre())
                .fechaAsociacion(guardada.getFechaAsociacion())
                .build();
    }

    public List<PublicacionUsuarioResponseDTO> obtenerAutores(Integer idPublicacion) {
        return publicacionUsuarioRepository.findByPublicacionId(idPublicacion)
                .stream()
                .map(pu -> PublicacionUsuarioResponseDTO.builder()
                        .id(pu.getId())
                        .nombrePublicacion(pu.getPublicacion().getTitulo())
                        .nombreUsuario(pu.getUsuario().getNombreCompleto())
                        .rolAutor(pu.getRolAutor().getNombre())
                        .fechaAsociacion(pu.getFechaAsociacion())
                        .build())
                .collect(Collectors.toList());
    }

    public void removerAutor(Integer idPublicacion, Integer idUsuario) {
        PublicacionUsuario asociacion = publicacionUsuarioRepository.findByPublicacionIdAndUsuarioId(idPublicacion, idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("PublicacionUsuario", "idUsuario", idUsuario));
        publicacionUsuarioRepository.delete(asociacion);
    }

    /**
     * Mapea entidad Publicacion a ResponseDTO
     */
    private PublicacionResponseDTO mapeoAResponseDTO(Publicacion publicacion) {
        List<String> autores = publicacionUsuarioRepository.findByPublicacionId(publicacion.getId())
                .stream()
                .map(pu -> pu.getUsuario().getNombreCompleto() + " (" + pu.getRolAutor().getNombre() + ")")
                .collect(Collectors.toList());

        String nombreProyecto = publicacion.getProyecto() != null ? publicacion.getProyecto().getTitulo() : null;
        String nombreSemillero = publicacion.getProyecto() != null && publicacion.getProyecto().getSemillero() != null
                ? publicacion.getProyecto().getSemillero().getNombre()
                : null;

        return PublicacionResponseDTO.builder()
                .id(publicacion.getId())
                .titulo(publicacion.getTitulo())
                .urlDocumento(publicacion.getUrlDocumento())
                .fechaPublicacion(publicacion.getFechaPublicacion())
                .nombreProyecto(nombreProyecto)
                .nombreSemillero(nombreSemillero)
                .autores(autores)
                .createdAt(publicacion.getCreatedAt())
                .build();
    }
}
