package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.EstudianteLineaInvestigacionResponseDTO;
import com.udea.agora_backend.dto.response.EstudianteResponseDTO;
import com.udea.agora_backend.model.Estudiante;
import com.udea.agora_backend.model.EstudianteLineaInvestigacion;
import com.udea.agora_backend.model.ProgramaAcademico;
import com.udea.agora_backend.model.Usuario;
import com.udea.agora_backend.repository.EstudianteHabilidadRepository;
import com.udea.agora_backend.repository.EstudianteLineaInvestigacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Convierte estudiantes y sus líneas de interés en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class EstudianteMapper {

    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final EstudianteHabilidadRepository estudianteHabilidadRepository;

    /** Incluye las líneas de investigación y las habilidades del perfil. */
    public EstudianteResponseDTO aResponse(Estudiante estudiante) {
        return aResponses(List.of(estudiante)).getFirst();
    }

    /** Convierte varios estudiantes con dos consultas en total: líneas y habilidades de todos. */
    public List<EstudianteResponseDTO> aResponses(List<Estudiante> estudiantes) {
        if (estudiantes.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = estudiantes.stream().map(Estudiante::getId).toList();
        Map<Integer, List<String>> lineas = estudianteLineaRepository.findByEstudianteIdIn(ids).stream()
                .collect(Collectors.groupingBy(el -> el.getEstudiante().getId(),
                        Collectors.mapping(el -> el.getLineaInvestigacion().getNombre(), Collectors.toList())));
        Map<Integer, List<String>> habilidades = estudianteHabilidadRepository.findByEstudianteIdIn(ids).stream()
                .collect(Collectors.groupingBy(eh -> eh.getEstudiante().getId(),
                        Collectors.mapping(eh -> eh.getHabilidad().getNombre(), Collectors.toList())));
        return estudiantes.stream()
                .map(e -> aResponse(e, lineas.getOrDefault(e.getId(), List.of()),
                        habilidades.getOrDefault(e.getId(), List.of())))
                .toList();
    }

    private EstudianteResponseDTO aResponse(Estudiante estudiante, List<String> lineas, List<String> habilidades) {
        Optional<Usuario> usuario = Optional.ofNullable(estudiante.getUsuario());

        return EstudianteResponseDTO.builder()
                .id(estudiante.getId())
                .nombreCompleto(usuario.map(Usuario::getNombreCompleto).orElse(null))
                .email(usuario.map(Usuario::getEmail).orElse(null))
                .nombrePrograma(Optional.ofNullable(estudiante.getPrograma()).map(ProgramaAcademico::getNombre).orElse(null))
                .semestre(estudiante.getSemestre())
                .lineasInvestigacion(lineas)
                .habilidades(habilidades)
                .createdAt(estudiante.getCreatedAt())
                .build();
    }

    public EstudianteLineaInvestigacionResponseDTO aLinea(EstudianteLineaInvestigacion asociacion) {
        return EstudianteLineaInvestigacionResponseDTO.builder()
                .id(asociacion.getId())
                .nombreEstudiante(asociacion.getEstudiante().getUsuario().getNombreCompleto())
                .nombreLineaInvestigacion(asociacion.getLineaInvestigacion().getNombre())
                .build();
    }
}
