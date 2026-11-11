package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.ConvocatoriaResponseDTO;
import com.udea.agora_backend.dto.response.RequisitoResponseDTO;
import com.udea.agora_backend.model.Convocatoria;
import com.udea.agora_backend.model.Estado;
import com.udea.agora_backend.model.RequisitoConvocatoria;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.repository.RequisitoConvocatoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Convierte convocatorias, con sus requisitos, en DTOs de respuesta. */
@Component
@RequiredArgsConstructor
public class ConvocatoriaMapper {

    private final RequisitoConvocatoriaRepository requisitoRepository;

    public ConvocatoriaResponseDTO aResponse(Convocatoria convocatoria) {
        return aResponses(List.of(convocatoria)).getFirst();
    }

    /** Convierte varias convocatorias con una sola consulta para los requisitos de todas. */
    public List<ConvocatoriaResponseDTO> aResponses(List<Convocatoria> convocatorias) {
        if (convocatorias.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<RequisitoResponseDTO>> requisitos = requisitoRepository
                .findByConvocatoriaIdIn(convocatorias.stream().map(Convocatoria::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getConvocatoria().getId(),
                        Collectors.mapping(ConvocatoriaMapper::aRequisito, Collectors.toList())));
        return convocatorias.stream()
                .map(c -> aResponse(c, requisitos.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    public static RequisitoResponseDTO aRequisito(RequisitoConvocatoria requisito) {
        return RequisitoResponseDTO.builder()
                .id(requisito.getId())
                .idHabilidad(requisito.esDeHabilidad() ? requisito.getHabilidad().getId() : null)
                .nombreHabilidad(requisito.esDeHabilidad() ? requisito.getHabilidad().getNombre() : null)
                .descripcion(requisito.getDescripcion())
                .obligatorio(requisito.isObligatorio())
                .build();
    }

    private ConvocatoriaResponseDTO aResponse(Convocatoria convocatoria, List<RequisitoResponseDTO> requisitos) {
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
                .requisitos(requisitos)
                .build();
    }
}
