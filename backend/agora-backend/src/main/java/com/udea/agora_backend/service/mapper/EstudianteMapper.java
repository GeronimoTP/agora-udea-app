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
import java.util.Optional;

/** Convierte estudiantes y sus líneas de interés en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class EstudianteMapper {

    private final EstudianteLineaInvestigacionRepository estudianteLineaRepository;
    private final EstudianteHabilidadRepository estudianteHabilidadRepository;

    /** Incluye las líneas de investigación y las habilidades del perfil. */
    public EstudianteResponseDTO aResponse(Estudiante estudiante) {
        List<String> lineas = estudianteLineaRepository.findByEstudianteId(estudiante.getId()).stream()
                .map(eli -> eli.getLineaInvestigacion().getNombre())
                .toList();
        List<String> habilidades = estudianteHabilidadRepository.findByEstudianteId(estudiante.getId()).stream()
                .map(eh -> eh.getHabilidad().getNombre())
                .toList();
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
