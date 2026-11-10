package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.ConvocatoriaResponseDTO;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.Semillero;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Convierte convocatorias en DTOs de respuesta. */
@Component
public class ConvocatoriaMapper {

    public ConvocatoriaResponseDTO aResponse(Convocatoria convocatoria) {
        Optional<Semillero> semillero = Optional.ofNullable(convocatoria.getSemillero());
        return ConvocatoriaResponseDTO.builder()
                .id(convocatoria.getId())
                .idSemillero(semillero.map(Semillero::getId).orElse(null))
                .titulo(convocatoria.getTitulo())
                .descripcion(convocatoria.getDescripcion())
                .fechaInicio(convocatoria.getFechaInicio())
                .fechaCierre(convocatoria.getFechaCierre())
                .cuposTotales(convocatoria.getCuposTotales())
                .cuposDisponibles(convocatoria.getCuposDisponibles())
                .nombreSemillero(semillero.map(Semillero::getNombre).orElse(null))
                .estadoActual(Optional.ofNullable(convocatoria.getEstado()).map(Estado::getNombre).orElse(null))
                .build();
    }
}
