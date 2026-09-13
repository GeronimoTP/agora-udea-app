package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolResponseDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
}
