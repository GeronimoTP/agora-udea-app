package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProyectoRequestDTO {

    @NotBlank(message = "El título del proyecto es obligatorio")
    private String titulo;

    @NotBlank(message = "La descripción del proyecto es obligatoria")
    private String descripcion;

    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;

    @NotNull(message = "El presupuesto asignado es obligatorio")
    @Min(value = 0, message = "El presupuesto no puede ser negativo")
    private Integer presupuestoAsignado;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;
}
