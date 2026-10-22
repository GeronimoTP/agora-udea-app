package com.udea.agora_backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ProfesorSemilleroResponseDTO {
    private Integer id;
    private String nombreSemillero;
    private Integer idProfesor;
    private Integer idUsuario;
    private String nombreProfesor;
    private String email;
    private LocalDate fechaAsignacion;
}
