package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SemilleroRequestDTO {

    @NotBlank(message = "El código identificador es obligatorio")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "El código solo debe contener letras mayúsculas, números y guiones")
    private String codigoIdentificador;

    @NotBlank(message = "El nombre del semillero es obligatorio")
    @Size(min = 5, max = 150, message = "El nombre debe tener entre 5 y 150 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "Se requiere asignar un estudiante líder")
    private Integer idEstudianteLider;

    @NotNull(message = "El programa académico es obligatorio")
    private Integer idPrograma;
}