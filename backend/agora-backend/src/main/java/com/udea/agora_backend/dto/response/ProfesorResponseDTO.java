package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class ProfesorResponseDTO {
    private Integer id;
    private String nombreCompleto;
    private String email;
    private String nombrePrograma;
    private ZonedDateTime createdAt;
    private List<String> areasEspecialidad;
}
