package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.ProyectoResponseDTO;
import com.udea.agora_backend.dto.response.VinculacionProyectoResponseDTO;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.EstudianteProyecto;
import com.udea.agora_backend.model.Proyecto;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.EstudianteProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Convierte proyectos y las vinculaciones de sus estudiantes en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class ProyectoMapper {

    private final EstudianteProyectoRepository estudianteProyectoRepository;

    public ProyectoResponseDTO aResponse(Proyecto proyecto) {
        Optional<Semillero> semillero = Optional.ofNullable(proyecto.getSemillero());
        return ProyectoResponseDTO.builder()
                .id(proyecto.getId())
                .idSemillero(semillero.map(Semillero::getId).orElse(null))
                .totalIntegrantes(estudianteProyectoRepository.findByProyectoId(proyecto.getId()).size())
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
