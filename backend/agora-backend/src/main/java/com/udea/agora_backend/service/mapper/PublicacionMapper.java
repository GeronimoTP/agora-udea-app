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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Convierte publicaciones y sus autorías en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class PublicacionMapper {

    private final PublicacionUsuarioRepository publicacionUsuarioRepository;
    private final EstudianteRepository estudianteRepository;

    /** Incluye los autores como texto ("Nombre (rol)") y con detalle para la vista. */
    public PublicacionResponseDTO aResponse(Publicacion publicacion) {
        return aResponses(List.of(publicacion)).getFirst();
    }

    /**
     * Convierte varias publicaciones con dos consultas en total: las autorías de todas y cuáles
     * de esos autores son estudiantes.
     */
    public List<PublicacionResponseDTO> aResponses(List<Publicacion> publicaciones) {
        if (publicaciones.isEmpty()) {
            return List.of();
        }
        List<PublicacionUsuario> todas = publicacionUsuarioRepository
                .findByPublicacionIdIn(publicaciones.stream().map(Publicacion::getId).toList());
        Map<Integer, List<PublicacionUsuario>> autoriasPorPublicacion = todas.stream()
                .collect(Collectors.groupingBy(pu -> pu.getPublicacion().getId()));
        Set<Integer> idsUsuarioEstudiante = todas.isEmpty()
                ? Set.of()
                : new HashSet<>(estudianteRepository.findIdsUsuarioEntre(
                        todas.stream().map(pu -> pu.getUsuario().getId()).collect(Collectors.toSet())));
        return publicaciones.stream()
                .map(p -> aResponse(p, autoriasPorPublicacion.getOrDefault(p.getId(), List.of()), idsUsuarioEstudiante))
                .toList();
    }

    private PublicacionResponseDTO aResponse(Publicacion publicacion, List<PublicacionUsuario> autorias,
                                             Set<Integer> idsUsuarioEstudiante) {
        List<String> autores = autorias.stream()
                .map(pu -> pu.getUsuario().getNombreCompleto() + " (" + pu.getRolAutor().getNombre() + ")")
                .toList();
        List<PublicacionResponseDTO.AutorDTO> autoresDetalle = autorias.stream()
                .map(autoria -> aAutor(autoria, idsUsuarioEstudiante))
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

    private PublicacionResponseDTO.AutorDTO aAutor(PublicacionUsuario autoria, Set<Integer> idsUsuarioEstudiante) {
        return PublicacionResponseDTO.AutorDTO.builder()
                .idUsuario(autoria.getUsuario().getId())
                .nombre(autoria.getUsuario().getNombreCompleto())
                .rolAutoria(autoria.getRolAutor().getNombre())
                .esEstudiante(idsUsuarioEstudiante.contains(autoria.getUsuario().getId()))
                .build();
    }
}
