package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ProfesorSemilleroResponseDTO {
    private Integer id;
    private String nombreSemillero;
    private String nombreProfesor;
    private LocalDate fechaAsignacion;
}
