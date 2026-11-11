package com.udea.agora_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de cierre es obligatoria")
    private LocalDate fechaCierre;

    @NotNull(message = "El ID del semillero es obligatorio")
    private Integer idSemillero;

    // Requisitos (habilidades del catálogo o texto libre, obligatorios u opcionales). Vacío o
    // ausente al crear: sin requisitos. Al editar, ausente (null) conserva los actuales.
    private List<@Valid RequisitoRequestDTO> requisitos;
}
