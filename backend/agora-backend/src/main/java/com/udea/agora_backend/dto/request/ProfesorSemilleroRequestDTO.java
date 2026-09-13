package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProfesorSemilleroRequestDTO {
    
    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;
    
    @NotNull(message = "El ID del profesor es obligatorio")
    private Integer idProfesor;
    
    @NotNull(message = "La fecha de asignación es obligatoria")
    private LocalDate fechaAsignacion;
}
