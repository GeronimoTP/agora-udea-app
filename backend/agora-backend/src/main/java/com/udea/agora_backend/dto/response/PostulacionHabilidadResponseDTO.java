package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostulacionHabilidadResponseDTO {
    private Integer id;
    private Integer idPostulacion;
    private String nombreHabilidad;
}
