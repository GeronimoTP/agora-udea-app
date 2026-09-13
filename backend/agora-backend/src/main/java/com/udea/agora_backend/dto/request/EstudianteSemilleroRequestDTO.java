package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EstudianteSemilleroRequestDTO {
    
    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;
    
    @NotNull(message = "El ID del estudiante es obligatorio")
    private Integer idEstudiante;
    
    @NotNull(message = "La fecha de ingreso es obligatoria")
    private LocalDate fechaIngreso;
    
    private LocalDate fechaSalida;
    
    @NotNull(message = "El estado de la vinculación es obligatorio")
    private Integer idEstado;
}
