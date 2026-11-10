package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.ProfesorAreaEspecialidadResponseDTO;
import com.udea.agora_backend.dto.response.ProfesorResponseDTO;
import com.udea.agora_backend.model.Profesor;
import com.udea.agora_backend.model.ProfesorAreaEspecialidad;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.ProfesorAreaEspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Convierte profesores y sus áreas de especialidad en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class ProfesorMapper {

    private final ProfesorAreaEspecialidadRepository profesorAreaRepository;

    /** Incluye las áreas de especialidad, el programa y la facultad. */
    public ProfesorResponseDTO aResponse(Profesor profesor) {
        List<String> areas = profesorAreaRepository.findByProfesorId(profesor.getId()).stream()
                .map(pae -> pae.getAreaEspecialidad().getNombre())
                .toList();
        Optional<Usuario> usuario = Optional.ofNullable(profesor.getUsuario());
        Optional<ProgramaAcademico> programa = Optional.ofNullable(profesor.getPrograma());

        return ProfesorResponseDTO.builder()
                .id(profesor.getId())
                .nombreCompleto(usuario.map(Usuario::getNombreCompleto).orElse(null))
                .email(usuario.map(Usuario::getEmail).orElse(null))
                .idPrograma(programa.map(ProgramaAcademico::getId).orElse(null))
                .nombrePrograma(programa.map(ProgramaAcademico::getNombre).orElse(null))
                .idFacultad(programa.map(p -> p.getFacultad().getId()).orElse(null))
                .nombreFacultad(programa.map(p -> p.getFacultad().getNombre()).orElse(null))
                .areasEspecialidad(areas)
                .createdAt(profesor.getCreatedAt())
                .build();
    }

    public ProfesorAreaEspecialidadResponseDTO aArea(ProfesorAreaEspecialidad asociacion) {
        return ProfesorAreaEspecialidadResponseDTO.builder()
                .id(asociacion.getId())
                .nombreProfesor(asociacion.getProfesor().getUsuario().getNombreCompleto())
                .nombreAreaEspecialidad(asociacion.getAreaEspecialidad().getNombre())
                .build();
    }
}
