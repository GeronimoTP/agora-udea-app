package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
@Builder
public class UsuarioResponseDTO {
    private Integer id;
    private String nombreCompleto;
    private String email;
    private String nombreRol;
    private ZonedDateTime createdAt;
}