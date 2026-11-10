package com.udea.agora_backend.service.mapper;

import com.udea.agora_backend.dto.response.ReconocimientoResponseDTO;
import com.udea.agora_backend.model.EntidadOtorgante;
import com.udea.agora_backend.model.ReconocimientoSemillero;
import com.udea.agora_backend.model.Semillero;
import com.udea.agora_backend.model.TipoReconocimiento;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Convierte reconocimientos de semilleros en DTOs de respuesta. */
@Component
public class ReconocimientoMapper {

    public ReconocimientoResponseDTO aResponse(ReconocimientoSemillero reconocimiento) {
        Optional<Semillero> semillero = Optional.ofNullable(reconocimiento.getSemillero());
        Optional<TipoReconocimiento> tipo = Optional.ofNullable(reconocimiento.getTipoReconocimiento());
        Optional<EntidadOtorgante> entidad = Optional.ofNullable(reconocimiento.getEntidadOtorgante());
        return ReconocimientoResponseDTO.builder()
                .id(reconocimiento.getId())
                .idSemillero(semillero.map(Semillero::getId).orElse(null))
                .nombreSemillero(semillero.map(Semillero::getNombre).orElse(null))
                .idTipoReconocimiento(tipo.map(TipoReconocimiento::getId).orElse(null))
                .tipoReconocimiento(tipo.map(TipoReconocimiento::getNombre).orElse(null))
                .titulo(reconocimiento.getTitulo())
                .descripcion(reconocimiento.getDescripcion())
                .idEntidadOtorgante(entidad.map(EntidadOtorgante::getId).orElse(null))
                .entidadOtorgante(entidad.map(EntidadOtorgante::getNombre).orElse(null))
                .fechaObtencion(reconocimiento.getFechaObtencion())
                .urlCertificado(reconocimiento.getUrlCertificado())
                .build();
    }
}
