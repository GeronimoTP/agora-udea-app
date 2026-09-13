package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class EstudianteRequestDTO {

    @NotNull(message = "El ID del usuario base es obligatorio")
    private Integer idUsuario;

    @NotNull(message = "El ID del programa académico es obligatorio")
    private Integer idPrograma;

    @NotNull(message = "El semestre es obligatorio")
    @Min(value = 1, message = "El semestre debe ser al menos 1")
    private Integer semestre;

    @NotEmpty(message = "Debe seleccionar al menos una línea de investigación de interés")
    private List<Integer> idLineasInvestigacion;
}
