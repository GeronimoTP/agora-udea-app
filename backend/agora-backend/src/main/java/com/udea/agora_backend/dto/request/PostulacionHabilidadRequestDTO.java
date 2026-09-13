package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostulacionHabilidadRequestDTO {
    
    @NotNull(message = "El ID de la postulación es obligatorio")
    private Integer idPostulacion;
    
    @NotNull(message = "El ID de la habilidad es obligatorio")
    private Integer idHabilidad;
}
