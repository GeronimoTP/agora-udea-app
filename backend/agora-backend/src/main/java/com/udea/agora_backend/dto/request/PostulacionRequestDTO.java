package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class PostulacionRequestDTO {

    @NotNull(message = "El ID de la convocatoria es obligatorio")
    private Integer idConvocatoria;

    @NotNull(message = "El ID del estudiante es obligatorio")
    private Integer idEstudiante;

    @NotBlank(message = "Debe incluir una respuesta de motivación")
    private String respuestaMotivacion;

    @NotNull(message = "Debe indicar su disponibilidad horaria")
    @Min(value = 2, message = "La disponibilidad mínima es de 2 horas a la semana")
    private Integer disponibilidadHorasSemana;

    @NotBlank(message = "La experiencia previa no puede estar vacía")
    private String experienciaPrevia;

    @NotEmpty(message = "Debe seleccionar al menos una habilidad")
    private List<Integer> idHabilidades;
}