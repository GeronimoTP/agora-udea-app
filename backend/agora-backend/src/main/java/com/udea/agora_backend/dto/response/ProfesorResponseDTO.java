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
    private Integer idPrograma;
    private String nombrePrograma;
    private Integer idFacultad;
    private String nombreFacultad;
    private ZonedDateTime createdAt;
    private List<String> areasEspecialidad;
}
