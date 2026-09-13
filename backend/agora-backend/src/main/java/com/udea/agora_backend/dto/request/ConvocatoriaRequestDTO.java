package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ConvocatoriaRequestDTO {

    @NotBlank(message = "El título de la convocatoria es obligatorio")
    private String titulo;

    @NotBlank(message = "La descripción de la convocatoria es obligatoria")
    private String descripcion;

    @NotNull(message = "Debe definir los cupos totales")
    @Min(value = 1, message = "Debe haber al menos 1 cupo disponible")
    private Integer cuposTotales;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede estar en el pasado")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de cierre es obligatoria")
    @Future(message = "La fecha de cierre debe ser en el futuro")
    private LocalDate fechaCierre;

    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;
}
