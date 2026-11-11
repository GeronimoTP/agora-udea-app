package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.ProyectoResponseDTO;
import com.udea.agora_backend.dto.response.VinculacionProyectoResponseDTO;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.EstudianteProyecto;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.EstudianteProyectoRepository;
import com.udea.agora_backend.repository.EstudianteProyectoRepository.IntegrantesDeProyecto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Convierte proyectos y las vinculaciones de sus estudiantes en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class ProyectoMapper {

    private final EstudianteProyectoRepository estudianteProyectoRepository;

    public ProyectoResponseDTO aResponse(Proyecto proyecto) {
        return aResponses(List.of(proyecto)).getFirst();
    }

    /** Convierte varios proyectos con una sola consulta agregada para contar sus integrantes. */
    public List<ProyectoResponseDTO> aResponses(List<Proyecto> proyectos) {
        if (proyectos.isEmpty()) {
            return List.of();
        }
        Map<Integer, Long> integrantes = estudianteProyectoRepository
                .contarPorProyecto(proyectos.stream().map(Proyecto::getId).toList()).stream()
                .collect(Collectors.toMap(IntegrantesDeProyecto::getIdProyecto, IntegrantesDeProyecto::getTotal));
        return proyectos.stream()
                .map(p -> aResponse(p, integrantes.getOrDefault(p.getId(), 0L).intValue()))
                .toList();
    }

    private ProyectoResponseDTO aResponse(Proyecto proyecto, int totalIntegrantes) {
        Optional<Semillero> semillero = Optional.ofNullable(proyecto.getSemillero());
        return ProyectoResponseDTO.builder()
                .id(proyecto.getId())
                .idSemillero(semillero.map(Semillero::getId).orElse(null))
                .totalIntegrantes(totalIntegrantes)
                .titulo(proyecto.getTitulo())
                .descripcion(proyecto.getDescripcion())
                .nombreSemillero(semillero.map(Semillero::getNombre).orElse(null))
                .presupuestoAsignado(proyecto.getPresupuestoAsignado())
                .fechaInicio(proyecto.getFechaInicio())
                .fechaFin(proyecto.getFechaFin())
                .estadoActual(Optional.ofNullable(proyecto.getEstado()).map(Estado::getNombre).orElse(null))
                .build();
    }

    public VinculacionProyectoResponseDTO aVinculacion(EstudianteProyecto vinculacion) {
        return VinculacionProyectoResponseDTO.builder()
                .id(vinculacion.getId())
                .tituloProyecto(vinculacion.getProyecto().getTitulo())
                .idEstudiante(vinculacion.getEstudiante().getId())
                .nombreEstudiante(vinculacion.getEstudiante().getUsuario().getNombreCompleto())
                .idRolProyecto(vinculacion.getRolProyecto().getId())
                .rolProyecto(vinculacion.getRolProyecto().getNombre())
                .fechaVinculacion(vinculacion.getFechaVinculacion())
                .build();
    }
}
