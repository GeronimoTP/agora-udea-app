package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.PublicacionResponseDTO;
import com.udea.agora_backend.dto.response.PublicacionUsuarioResponseDTO;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Publicacion;
import com.udea.agora_backend.model.PublicacionUsuario;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.EstudianteRepository;
import com.udea.agora_backend.repository.PublicacionUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Convierte publicaciones y sus autorías en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class PublicacionMapper {

    private final PublicacionUsuarioRepository publicacionUsuarioRepository;
    private final EstudianteRepository estudianteRepository;

    /** Incluye los autores como texto ("Nombre (rol)") y con detalle para la vista. */
    public PublicacionResponseDTO aResponse(Publicacion publicacion) {
        List<PublicacionUsuario> autorias = publicacionUsuarioRepository.findByPublicacionId(publicacion.getId());
        List<String> autores = autorias.stream()
                .map(pu -> pu.getUsuario().getNombreCompleto() + " (" + pu.getRolAutor().getNombre() + ")")
                .toList();
        List<PublicacionResponseDTO.AutorDTO> autoresDetalle = autorias.stream()
                .map(this::aAutor)
                .toList();
        Optional<Proyecto> proyecto = Optional.ofNullable(publicacion.getProyecto());

        return PublicacionResponseDTO.builder()
                .id(publicacion.getId())
                .titulo(publicacion.getTitulo())
                .urlDocumento(publicacion.getUrlDocumento())
                .fechaPublicacion(publicacion.getFechaPublicacion())
                .idProyecto(proyecto.map(Proyecto::getId).orElse(null))
                .nombreProyecto(proyecto.map(Proyecto::getTitulo).orElse(null))
                .nombreSemillero(proyecto.map(Proyecto::getSemillero).map(Semillero::getNombre).orElse(null))
                .autores(autores)
                .autoresDetalle(autoresDetalle)
                .createdAt(publicacion.getCreatedAt())
                .build();
    }

    public PublicacionUsuarioResponseDTO aAutoria(PublicacionUsuario autoria) {
        return PublicacionUsuarioResponseDTO.builder()
                .id(autoria.getId())
                .nombrePublicacion(autoria.getPublicacion().getTitulo())
                .idUsuario(autoria.getUsuario().getId())
                .nombreUsuario(autoria.getUsuario().getNombreCompleto())
                .rolAutor(autoria.getRolAutor().getNombre())
                .fechaAsociacion(autoria.getFechaAsociacion())
                .build();
    }

    private PublicacionResponseDTO.AutorDTO aAutor(PublicacionUsuario autoria) {
        return PublicacionResponseDTO.AutorDTO.builder()
                .idUsuario(autoria.getUsuario().getId())
                .nombre(autoria.getUsuario().getNombreCompleto())
                .rolAutoria(autoria.getRolAutor().getNombre())
                .esEstudiante(estudianteRepository.findByUsuarioId(autoria.getUsuario().getId()).isPresent())
                .build();
    }
}
