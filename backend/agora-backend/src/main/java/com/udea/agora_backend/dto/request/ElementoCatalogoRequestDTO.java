package com.udea.agora_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Alta de una entidad otorgante o de un tipo de reconocimiento (solo administrador). */
@Data
public class ElementoCatalogoRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "Máximo 150 caracteres")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String descripcion;
}
